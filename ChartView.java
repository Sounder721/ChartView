package com.huicheng.jingkaiqu.view.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.huicheng.jingkaiqu.R;
import com.huicheng.jingkaiqu.uitl.Logger;

import java.util.ArrayList;

/**
 * 图表view,已实现直方图和扇形图
 */
public class ChartView extends View {
    /**柱状图*/
    public static final int CHART_TYPE_HISTOGRAM = 101;
    /**扇形图*/
    public static final int CHART_TYPE_FAN = 102;
    /**动画时长*/
    public static final int DURATION = 1000;
    /** 图表上描述文字颜色*/
    private int mTextColor;
    private float mTextSize;
    /**图标类型*/
    private int mChartType;
    /**柱状图时，每列的宽度与间隔的宽度的比例,默认为5*/
    private int mProportion;
    /**坐标轴的颜色，宽度就默认为1px了*/
    private int mAxisColor;
    /**区间分割线的颜色*/
    private int mSectionColor;
    /**区间个数,柱状图里，最大数值的entity占（count-1）/count 的比例*/
    private int mSectionAmount;


    private TextPaint mTextPaint;
    private Paint mChartPaint;
    private float mTextWidth;
    private float mTextHeight;
    /**
     * 动画进度值[0f,1f],不同图表类型有不同计算方式
     * 默认为1，即表示动画已完成
     */
    private float mAnimValue = 1f;
    private ValueAnimator mValueAnimator;

    private ArrayList<Entity> mEntities;

    public ChartView(Context context) {
        super(context);
        init(null, 0);
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ChartView, defStyle, 0);

        mTextColor = a.getColor(
                R.styleable.ChartView_textColor,
                Color.BLACK);
        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mTextSize = a.getDimension(
                R.styleable.ChartView_textSize,20);
        mChartType = a.getInt(R.styleable.ChartView_chartType,CHART_TYPE_HISTOGRAM);

        mProportion = a.getInt(R.styleable.ChartView_proportion,5);
        mAxisColor = a.getColor(R.styleable.ChartView_axisColor,Color.parseColor("#888888"));
        mSectionColor = a.getColor(R.styleable.ChartView_sectionColor,Color.parseColor("#e6e6e6"));
        mSectionAmount = a.getInt(R.styleable.ChartView_sectionAmount,4);
        a.recycle();


        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mChartPaint = new Paint();
        mChartPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mChartPaint.setStyle(Paint.Style.FILL);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mTextColor);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        //默认以w/h : 16/12 的形式展现
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(screenWidth, (int) (screenWidth*12f/16f));
        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(screenWidth, heightSpecSize);
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, (int) (screenWidth*12f/16f));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mEntities == null || mEntities.size() == 0){
            return;
        }
        if(mChartType == CHART_TYPE_HISTOGRAM){
            onDrawHistogram(canvas);
        }else if(mChartType == CHART_TYPE_FAN){
            onoDrawFan(canvas);
        }
    }
    private void onoDrawFan(Canvas canvas){
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        int length = mEntities.size();
        //计算扇形大小，
        //圆心坐标
        Point pCenter = new Point(getWidth()/2,getHeight()/2);
        //半径
        //
        int min = contentWidth > contentHeight ? contentHeight :contentWidth;
        float raduis =  (min/2f)*(mProportion-1)/mProportion;
        //计算总值，用以计算每个entity所占比例
        int totalAmount = 0;
        for(int i = 0; i<length;i++){
            totalAmount += mEntities.get(i).getAmount();
        }
        double startAngle = -90;
        RectF rectF = new RectF(pCenter.x-raduis,pCenter.y-raduis,pCenter.x+raduis,pCenter.y+raduis);
        mChartPaint.setStrokeWidth(2);
        mChartPaint.setTextSize(mTextSize);
        for(int i = 0; i<length;i++){
            Entity e = mEntities.get(i);
            //画扇形
            double sweepAngle = e.getAmount() *1f / totalAmount * 360 * mAnimValue;
            mChartPaint.setColor(e.getColor());
            canvas.drawArc(rectF,(float)startAngle,(float)sweepAngle,true,mChartPaint);
            //画文字和折线
            /*
                折线从每个项的弧形中间出发，向外一定距离后横向一定距离再绘制文字
                折线需要三个点
             */
            //起点
            double vCos = Math.cos((startAngle+sweepAngle/2) * 2 * Math.PI / 360);
            double vSin = Math.sin((startAngle+sweepAngle/2) * 2 * Math.PI / 360);
            int startX = (int) (vCos * raduis)+pCenter.x;
            int startY = (int) (vSin * raduis)+pCenter.y;
//            //中点，计算中点就是把半径增到一定距离
            int middleX = (int) (vCos * (raduis+30))+pCenter.x;
            int middleY = (int) (vSin * (raduis+30))+pCenter.y;
            //终点，需要根据角度来判断加还是减
            int endY = middleY;
            double angle = (startAngle+sweepAngle/2)%360;
            int endX = middleX;
            boolean left = false;
            if(angle >= 270 || angle <= 90){
                endX += 50;
                left = false;
            }else{
                endX -= 50;
                left = true;
            }
            canvas.drawLine(startX,startY,middleX,middleY,mChartPaint);
            canvas.drawLine(middleX,middleY,endX,endY,mChartPaint);
            //以终点为基础绘制文字
            float w = mTextPaint.measureText(e.getText());
            w = left ? w*-1.2f : w/1.8f;//数值是随便调的
            canvas.drawText(e.getText(),endX+w,endY+mTextSize/2,mChartPaint);
            startAngle += sweepAngle;
        }
    }
    private void onDrawHistogram(Canvas canvas){
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        int column = mEntities.size();
        int maxAmount = 0;
        for(Entity e : mEntities){
            maxAmount = maxAmount < e.getAmount() ? e.getAmount() : maxAmount;
        }
        float n = (mSectionAmount-1)*1f/mSectionAmount;
        float perUnitHeight = contentHeight * n /maxAmount;
        //计算每列的宽度
        int distance = contentWidth / ((mProportion+1)*column+1);
        //开始绘制

        //再绘制四条区间线
        mChartPaint.setColor(mSectionColor);
        float unitSectionHeight = (contentHeight) / mSectionAmount;
        for(int i=0;i<mSectionAmount;i++){
            canvas.drawLine(paddingLeft,paddingTop+unitSectionHeight*i,contentWidth,paddingTop+unitSectionHeight*i+1,mChartPaint);
        }
        //绘制柱状图
        int startX = paddingLeft;
        for (int i = 0;i < column ;i++){
            Entity e = mEntities.get(i);
            mChartPaint.setColor(e.getColor());
            float h = contentHeight - perUnitHeight*e.getAmount();
            canvas.drawRect(startX,contentHeight - mAnimValue * (contentHeight-h),startX+mProportion*distance,contentHeight,mChartPaint);
            canvas.drawText(e.getText(),0,e.getText().length(),startX+mProportion/2f*distance,h-2*mTextSize,mTextPaint);
            String _amount = String.valueOf(e.getAmount());
            canvas.drawText(_amount,0,_amount.length(),startX+mProportion/2f*distance,h-mTextSize,mTextPaint);
            startX += (mProportion+1)*distance;
        }

        //最后画绘制坐标轴
        mChartPaint.setColor(mAxisColor);
        //y轴
        canvas.drawLine(paddingLeft,paddingTop,paddingLeft+1,contentHeight,mChartPaint);
        //x轴
        canvas.drawLine(paddingLeft,contentHeight-1,contentWidth,contentHeight,mChartPaint);
    }
    public void setEntities(ArrayList<Entity> entities){
        this.mEntities = entities;
        postInvalidate();
    }
    public void startAnimation(){
        mValueAnimator = ValueAnimator.ofFloat(0f,1f);
        mValueAnimator.setDuration(DURATION);
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimValue = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        mValueAnimator.start();
    }
    public static class Entity{
        private String text;
        private int amount;
        private int color;
        public Entity(String text, int amount, int color) {
            this.text = text;
            this.amount = amount;
            this.color = color;
        }
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public int getColor() {
            return color;
        }

        public void setColor(int color) {
            this.color = color;
        }
    }
}
