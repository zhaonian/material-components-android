package com.google.android.material.button;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.MarginLayoutParamsCompat;

import com.google.android.material.internal.ViewUtils;
import com.google.android.material.shape.AbsoluteCornerSize;
import com.google.android.material.shape.CornerSize;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.ArrayList;
import java.util.List;

public class MaterialButtonGroup extends LinearLayout {
  public MaterialButtonGroup(Context context) {
    super(context);
  }

  public MaterialButtonGroup(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public MaterialButtonGroup(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  private final List<MaterialButtonGroup.CornerData> originalCornerData = new ArrayList<>();

  /**
   * This override prohibits Views other than {@link MaterialButton} to be added. It also makes
   * updates to the add button shape and margins.
   */
  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    if (!(child instanceof MaterialButton)) {
      return;
    }

    super.addView(child, index, params);
    MaterialButton buttonChild = (MaterialButton) child;
//    setGeneratedIdIfNeeded(buttonChild);
    // Sets sensible default values and an internal checked change listener for this child
//    setupButtonChild(buttonChild);

    // Reorders children if a checked child was added to this layout
//    if (buttonChild.isChecked()) {
//      updateCheckedStates(buttonChild.getId(), true);
//      setCheckedId(buttonChild.getId());
//    }

    // Saves original corner data
    ShapeAppearanceModel shapeAppearanceModel = buttonChild.getShapeAppearanceModel();
    originalCornerData.add(
        new MaterialButtonGroup.CornerData(
            shapeAppearanceModel.getTopLeftCornerSize(),
            shapeAppearanceModel.getBottomLeftCornerSize(),
            shapeAppearanceModel.getTopRightCornerSize(),
            shapeAppearanceModel.getBottomRightCornerSize()));
  }
//  @Override
//  public void onViewRemoved(View child) {
//    super.onViewRemoved(child);
//
//    if (child instanceof MaterialButton) {
//      ((MaterialButton) child).removeOnCheckedChangeListener(checkedStateTracker);
//      ((MaterialButton) child).setOnPressedChangeListenerInternal(null);
//    }
//
//    int indexOfChild = indexOfChild(child);
//    if (indexOfChild >= 0) {
//      originalCornerData.remove(indexOfChild);
//    }
//
//    updateChildShapes();
//    adjustChildMarginsAndUpdateLayout();
//  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    updateChildShapes();
    adjustChildMarginsAndUpdateLayout();

    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  /**
   * Sets a negative marginStart on all but the first child, if two adjacent children both have a
   * stroke width greater than 0. This prevents a double-width stroke from being drawn for two
   * adjacent stroked children, and instead draws the adjacent strokes directly on top of each
   * other.
   *
   * <p>The negative margin adjustment amount will be equal to the smaller of the two adjacent
   * stroke widths.
   *
   * <p>Also rearranges children such that they are shown in the correct visual order.
   */
  private void adjustChildMarginsAndUpdateLayout() {
    int firstVisibleChildIndex = getFirstVisibleChildIndex();
    if (firstVisibleChildIndex == -1) {
      return;
    }

    for (int i = firstVisibleChildIndex + 1; i < getChildCount(); i++) {
      // Only adjusts margins if both adjacent children are MaterialButtons
      MaterialButton currentButton = getChildButton(i);
      MaterialButton previousButton = getChildButton(i - 1);

      // Calculates the margin adjustment to be the smaller of the two adjacent stroke widths
      int smallestStrokeWidth =
          Math.min(currentButton.getStrokeWidth(), previousButton.getStrokeWidth());

      LayoutParams params = buildLayoutParams(currentButton);
      if (getOrientation() == HORIZONTAL) {
        MarginLayoutParamsCompat.setMarginEnd(params, 0);
        MarginLayoutParamsCompat.setMarginStart(params, -smallestStrokeWidth);
      } else {
        params.bottomMargin = 0;
        params.topMargin = -smallestStrokeWidth;
      }

      currentButton.setLayoutParams(params);
    }

    resetChildMargins(firstVisibleChildIndex);
  }

  private MaterialButton getChildButton(int index) {
    return (MaterialButton) getChildAt(index);
  }

  private void resetChildMargins(int childIndex) {
    if (getChildCount() == 0 || childIndex == -1) {
      return;
    }

    MaterialButton currentButton = getChildButton(childIndex);
    LayoutParams params = (LayoutParams) currentButton.getLayoutParams();
    if (getOrientation() == VERTICAL) {
      params.topMargin = 0;
      params.bottomMargin = 0;
      return;
    }

    MarginLayoutParamsCompat.setMarginEnd(params, 0);
    MarginLayoutParamsCompat.setMarginStart(params, 0);
    params.leftMargin = 0;
    params.rightMargin = 0;
  }

  @NonNull
  private LinearLayout.LayoutParams buildLayoutParams(@NonNull View child) {
    ViewGroup.LayoutParams layoutParams = child.getLayoutParams();
    if (layoutParams instanceof LinearLayout.LayoutParams) {
      return (LayoutParams) layoutParams;
    }

    return new LayoutParams(layoutParams.width, layoutParams.height);
  }

  /** Sets all corner radius to 0 except for leftmost and rightmost corners. */
  @VisibleForTesting
  void updateChildShapes() {
    int childCount = getChildCount();
    int firstVisibleChildIndex = getFirstVisibleChildIndex();
    int lastVisibleChildIndex = getLastVisibleChildIndex();
    for (int i = 0; i < childCount; i++) {
      MaterialButton button = getChildButton(i);
      if (button.getVisibility() == GONE) {
        continue;
      }

      ShapeAppearanceModel.Builder builder =
          button.getShapeAppearanceModel().toBuilder();
      MaterialButtonGroup.CornerData newCornerData = getNewCornerData(i, firstVisibleChildIndex, lastVisibleChildIndex);
      updateBuilderWithCornerData(builder, newCornerData);

      button.setShapeAppearanceModel(builder.build());
    }
  }

  private int getFirstVisibleChildIndex() {
    int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      if (isChildVisible(i)) {
        return i;
      }
    }

    return -1;
  }

  private int getLastVisibleChildIndex() {
    int childCount = getChildCount();
    for (int i = childCount - 1; i >= 0; i--) {
      if (isChildVisible(i)) {
        return i;
      }
    }

    return -1;
  }

  private boolean isChildVisible(int i) {
    View child = getChildAt(i);
    return child.getVisibility() != View.GONE;
  }

  @Nullable
  private MaterialButtonGroup.CornerData getNewCornerData(
      int index,
      int firstVisibleChildIndex,
      int lastVisibleChildIndex) {
    int childCount = getChildCount();
    MaterialButtonGroup.CornerData cornerData = originalCornerData.get(index);
    if (childCount == 1) {
      return cornerData;
    }

    boolean isHorizontal = getOrientation() == HORIZONTAL;
    if (index == firstVisibleChildIndex) {
      return isHorizontal ? MaterialButtonGroup.CornerData.start(cornerData, this) : MaterialButtonGroup.CornerData.top(cornerData);
    }

    if (index == lastVisibleChildIndex) {
      return isHorizontal ? MaterialButtonGroup.CornerData.end(cornerData, this) : MaterialButtonGroup.CornerData.bottom(cornerData);
    }

    return null;
  }

  private static void updateBuilderWithCornerData(
      ShapeAppearanceModel.Builder shapeAppearanceModelBuilder, @Nullable MaterialButtonGroup.CornerData cornerData) {
    if (cornerData == null) {
      shapeAppearanceModelBuilder.setAllCornerSizes(0);
      return;
    }

    shapeAppearanceModelBuilder
        .setTopLeftCornerSize(cornerData.topLeft)
        .setBottomLeftCornerSize(cornerData.bottomLeft)
        .setTopRightCornerSize(cornerData.topRight)
        .setBottomRightCornerSize(cornerData.bottomRight);
  }
  
  private static class CornerData {

    private static final CornerSize noCorner = new AbsoluteCornerSize(0);

    CornerSize topLeft;
    CornerSize topRight;
    CornerSize bottomRight;
    CornerSize bottomLeft;

    CornerData(
        CornerSize topLeft, CornerSize bottomLeft, CornerSize topRight, CornerSize bottomRight) {
      this.topLeft = topLeft;
      this.topRight = topRight;
      this.bottomRight = bottomRight;
      this.bottomLeft = bottomLeft;
    }

    /** Keep the start side of the corner original data */
    public static MaterialButtonGroup.CornerData start(MaterialButtonGroup.CornerData orig, View view) {
      return ViewUtils.isLayoutRtl(view) ? right(orig) : left(orig);
    }

    /** Keep the end side of the corner original data */
    public static MaterialButtonGroup.CornerData end(MaterialButtonGroup.CornerData orig, View view) {
      return ViewUtils.isLayoutRtl(view) ? left(orig) : right(orig);
    }

    /** Keep the left side of the corner original data */
    public static MaterialButtonGroup.CornerData left(MaterialButtonGroup.CornerData orig) {
      return new MaterialButtonGroup.CornerData(orig.topLeft, orig.bottomLeft, noCorner, noCorner);
    }

    /** Keep the right side of the corner original data */
    public static MaterialButtonGroup.CornerData right(MaterialButtonGroup.CornerData orig) {
      return new MaterialButtonGroup.CornerData(noCorner, noCorner, orig.topRight, orig.bottomRight);
    }

    /** Keep the top side of the corner original data */
    public static MaterialButtonGroup.CornerData top(MaterialButtonGroup.CornerData orig) {
      return new MaterialButtonGroup.CornerData(orig.topLeft, noCorner, orig.topRight, noCorner);
    }

    /** Keep the bottom side of the corner original data */
    public static MaterialButtonGroup.CornerData bottom(MaterialButtonGroup.CornerData orig) {
      return new MaterialButtonGroup.CornerData(noCorner, orig.bottomLeft, noCorner, orig.bottomRight);
    }
  }
}
