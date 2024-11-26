package com.example.onthelamp.ui
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class DividerItemDecoration(private val color: Int, private val height: Int) : RecyclerView.ItemDecoration() {
    private val paint = Paint()

    init {
        paint.color = color
        paint.style = Paint.Style.FILL
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount
        for (i in 0 until childCount - 1) { // 마지막 아이템 아래는 구분선 추가 X
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val left = parent.paddingLeft
            val right = parent.width - parent.paddingRight
            val top = child.bottom + params.bottomMargin
            val bottom = top + height
            c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.bottom = height // 아이템 간 간격 추가
    }
}
