/*
 * Copyright (C) 2016 Álinson Santos Xavier <isoron@gmail.com>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.ui.habits.show.views;

import android.content.*;
import android.graphics.*;
import android.util.*;

import org.isoron.uhabits.*;
import org.isoron.uhabits.models.*;
import org.isoron.uhabits.tasks.*;
import org.isoron.uhabits.utils.*;

import java.text.*;
import java.util.*;

public class HabitFrequencyView extends ScrollableDataView
    implements HabitDataView, ModelObservable.Listener
{
    private Paint pGrid;

    private float em;

    private Habit habit;

    private SimpleDateFormat dfMonth;

    private SimpleDateFormat dfYear;

    private Paint pText, pGraph;

    private RectF rect, prevRect;

    private int baseSize;

    private int paddingTop;

    private float columnWidth;

    private int columnHeight;

    private int nColumns;

    private int textColor;

    private int gridColor;

    private int[] colors;

    private int primaryColor;

    private boolean isBackgroundTransparent;

    private HashMap<Long, Integer[]> frequency;

    public HabitFrequencyView(Context context)
    {
        super(context);
        init();
    }

    public HabitFrequencyView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.primaryColor = ColorUtils.getColor(getContext(), 7);
        this.frequency = new HashMap<>();
        init();
    }

    @Override
    public void onModelChange()
    {
        refreshData();
    }

    public void refreshData()
    {
        if (isInEditMode()) generateRandomData();
        else if (habit != null)
        {
            frequency = habit.getRepetitions().getWeekdayFrequency();
            createColors();
        }

        postInvalidate();
    }

    public void setHabit(Habit habit)
    {
        this.habit = habit;
        createColors();
    }

    public void setIsBackgroundTransparent(boolean isBackgroundTransparent)
    {
        this.isBackgroundTransparent = isBackgroundTransparent;
        createColors();
    }

    protected void createPaints()
    {
        pText = new Paint();
        pText.setAntiAlias(true);

        pGraph = new Paint();
        pGraph.setTextAlign(Paint.Align.CENTER);
        pGraph.setAntiAlias(true);

        pGrid = new Paint();
        pGrid.setAntiAlias(true);
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        new BaseTask()
        {
            @Override
            protected void doInBackground()
            {
                refreshData();
            }
        }.execute();
        habit.getObservable().addListener(this);
        habit.getCheckmarks().observable.addListener(this);
    }

    @Override
    protected void onDetachedFromWindow()
    {
        habit.getCheckmarks().observable.removeListener(this);
        habit.getObservable().removeListener(this);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        rect.set(0, 0, nColumns * columnWidth, columnHeight);
        rect.offset(0, paddingTop);

        drawGrid(canvas, rect);

        pText.setTextAlign(Paint.Align.CENTER);
        pText.setColor(textColor);
        pGraph.setColor(primaryColor);
        prevRect.setEmpty();

        GregorianCalendar currentDate = DateUtils.getStartOfTodayCalendar();

        currentDate.set(Calendar.DAY_OF_MONTH, 1);
        currentDate.add(Calendar.MONTH, -nColumns + 2 - getDataOffset());

        for (int i = 0; i < nColumns - 1; i++)
        {
            rect.set(0, 0, columnWidth, columnHeight);
            rect.offset(i * columnWidth, 0);

            drawColumn(canvas, rect, currentDate);
            currentDate.add(Calendar.MONTH, 1);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int width,
                                 int height,
                                 int oldWidth,
                                 int oldHeight)
    {
        if (height < 9) height = 200;

        baseSize = height / 8;
        setScrollerBucketSize(baseSize);

        pText.setTextSize(baseSize * 0.4f);
        pGraph.setTextSize(baseSize * 0.4f);
        pGraph.setStrokeWidth(baseSize * 0.1f);
        pGrid.setStrokeWidth(baseSize * 0.05f);
        em = pText.getFontSpacing();

        columnWidth = baseSize;
        columnWidth = Math.max(columnWidth, getMaxMonthWidth() * 1.2f);

        columnHeight = 8 * baseSize;
        nColumns = (int) (width / columnWidth);
        paddingTop = 0;
    }

    private void createColors()
    {
        if (habit != null)
        {
            this.primaryColor =
                ColorUtils.getColor(getContext(), habit.getColor());
        }

        textColor = InterfaceUtils.getStyledColor(getContext(),
            R.attr.mediumContrastTextColor);
        gridColor = InterfaceUtils.getStyledColor(getContext(),
            R.attr.lowContrastTextColor);

        colors = new int[4];
        colors[0] = gridColor;
        colors[3] = primaryColor;
        colors[1] = ColorUtils.mixColors(colors[0], colors[3], 0.66f);
        colors[2] = ColorUtils.mixColors(colors[0], colors[3], 0.33f);
    }

    private void drawColumn(Canvas canvas, RectF rect, GregorianCalendar date)
    {
        Integer values[] = frequency.get(date.getTimeInMillis());
        float rowHeight = rect.height() / 8.0f;
        prevRect.set(rect);

        Integer[] localeWeekdayList = DateUtils.getLocaleWeekdayList();
        for (int j = 0; j < localeWeekdayList.length; j++)
        {
            rect.set(0, 0, baseSize, baseSize);
            rect.offset(prevRect.left, prevRect.top + baseSize * j);

            int i = DateUtils.javaWeekdayToLoopWeekday(localeWeekdayList[j]);
            if (values != null) drawMarker(canvas, rect, values[i]);

            rect.offset(0, rowHeight);
        }

        drawFooter(canvas, rect, date);
    }

    private void drawFooter(Canvas canvas, RectF rect, GregorianCalendar date)
    {
        Date time = date.getTime();

        canvas.drawText(dfMonth.format(time), rect.centerX(),
            rect.centerY() - 0.1f * em, pText);

        if (date.get(Calendar.MONTH) == 1)
            canvas.drawText(dfYear.format(time), rect.centerX(),
                rect.centerY() + 0.9f * em, pText);
    }

    private void drawGrid(Canvas canvas, RectF rGrid)
    {
        int nRows = 7;
        float rowHeight = rGrid.height() / (nRows + 1);

        pText.setTextAlign(Paint.Align.LEFT);
        pText.setColor(textColor);
        pGrid.setColor(gridColor);

        for (String day : DateUtils.getLocaleDayNames(Calendar.SHORT))
        {
            canvas.drawText(day, rGrid.right - columnWidth,
                rGrid.top + rowHeight / 2 + 0.25f * em, pText);

            pGrid.setStrokeWidth(1f);
            canvas.drawLine(rGrid.left, rGrid.top, rGrid.right, rGrid.top,
                pGrid);

            rGrid.offset(0, rowHeight);
        }

        canvas.drawLine(rGrid.left, rGrid.top, rGrid.right, rGrid.top, pGrid);
    }

    private void drawMarker(Canvas canvas, RectF rect, Integer value)
    {
        float padding = rect.height() * 0.2f;
        float radius =
            (rect.height() - 2 * padding) / 2.0f / 4.0f * Math.min(value, 4);

        pGraph.setColor(colors[Math.min(3, Math.max(0, value - 1))]);
        canvas.drawCircle(rect.centerX(), rect.centerY(), radius, pGraph);
    }

    private void generateRandomData()
    {
        GregorianCalendar date = DateUtils.getStartOfTodayCalendar();
        date.set(Calendar.DAY_OF_MONTH, 1);
        Random rand = new Random();
        frequency.clear();

        for (int i = 0; i < 40; i++)
        {
            Integer values[] = new Integer[7];
            for (int j = 0; j < 7; j++)
                values[j] = rand.nextInt(5);

            frequency.put(date.getTimeInMillis(), values);
            date.add(Calendar.MONTH, -1);
        }
    }

    private float getMaxMonthWidth()
    {
        float maxMonthWidth = 0;
        GregorianCalendar day = DateUtils.getStartOfTodayCalendar();

        for (int i = 0; i < 12; i++)
        {
            day.set(Calendar.MONTH, i);
            float monthWidth = pText.measureText(dfMonth.format(day.getTime()));
            maxMonthWidth = Math.max(maxMonthWidth, monthWidth);
        }

        return maxMonthWidth;
    }

    private void init()
    {
        createPaints();
        createColors();

        dfMonth = DateUtils.getDateFormat("MMM");
        dfYear = DateUtils.getDateFormat("yyyy");

        rect = new RectF();
        prevRect = new RectF();
    }
}
