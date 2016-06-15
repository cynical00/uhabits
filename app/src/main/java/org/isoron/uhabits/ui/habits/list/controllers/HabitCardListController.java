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

package org.isoron.uhabits.ui.habits.list.controllers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mobeta.android.dslv.DragSortListView;

import org.isoron.uhabits.models.Habit;
import org.isoron.uhabits.ui.habits.list.model.HabitCardListAdapter;
import org.isoron.uhabits.ui.habits.list.views.HabitCardListView;

/**
 * Controller responsible for receiving and processing the events generated by a
 * HabitListView. These include selecting and reordering items, toggling
 * checkmarks and clicking habits.
 */
public class HabitCardListController implements DragSortListView.DropListener,
                                                DragSortListView.DragListener,
                                                HabitCardListView.Controller
{
    private final Mode NORMAL_MODE = new NormalMode();

    private final Mode SELECTION_MODE = new SelectionMode();

    @NonNull
    private final HabitCardListAdapter adapter;

    @NonNull
    private final HabitCardListView view;

    @Nullable
    private HabitListener habitListener;

    @Nullable
    private SelectionListener selectionListener;

    @NonNull
    private Mode activeMode;

    public HabitCardListController(@NonNull HabitCardListAdapter adapter,
                                   @NonNull HabitCardListView view)
    {
        this.adapter = adapter;
        this.view = view;
        this.activeMode = new NormalMode();
    }

    /**
     * Called when the user is dragging a habit which was originally at position
     * 'from' and is currently hovering over position 'to'. Note that the user
     * has not yet finished the dragging operation.
     *
     * @param from the original position of the habit
     * @param to   the position where the habit is currently hovering
     */
    @Override
    public void drag(int from, int to)
    {
        // ignored
    }

    /**
     * Called when the user drags a habit and drops it somewhere. Note that the
     * dragging operation is already complete.
     *
     * @param from the original position of the habit
     * @param to   the position where the habit was released
     */
    @Override
    public void drop(int from, int to)
    {
        if (from == to) return;
        cancelSelection();

        Habit habitFrom = adapter.getItem(from);
        Habit habitTo = adapter.getItem(to);
        adapter.reorder(from, to);

        if (habitListener != null)
            habitListener.onHabitReorder(habitFrom, habitTo);
    }

    /**
     * Called when the user attempts to perform a toggle, but attempt is
     * rejected.
     */
    @Override
    public void onInvalidToggle()
    {
        if (habitListener != null) habitListener.onInvalidToggle();
    }

    /**
     * Called when the user clicks at some item.
     *
     * @param position the position of the clicked item
     */
    @Override
    public void onItemClick(int position)
    {
        activeMode.onItemClick(position);
    }

    /**
     * Called when the user long clicks at some item.
     *
     * @param position the position of the clicked item
     */
    @Override
    public void onItemLongClick(int position)
    {
        activeMode.onItemLongClick(position);
    }

    /**
     * Called when the user wants to toggle a checkmark.
     *
     * @param habit     the habit of the checkmark
     * @param timestamp the timestamps of the checkmark
     */
    @Override
    public void onToggle(Habit habit, long timestamp)
    {
        if (habitListener != null) habitListener.onToggle(habit, timestamp);
    }

    public void setHabitListener(@Nullable HabitListener habitListener)
    {
        this.habitListener = habitListener;
    }

    public void setSelectionListener(@Nullable SelectionListener listener)
    {
        this.selectionListener = listener;
    }


    /**
     * Called when the user starts dragging an item.
     *
     * @param position the position of the habit dragged
     */
    @Override
    public void startDrag(int position)
    {
        activeMode.startDrag(position);
    }

    /**
     * Marks all items as not selected and finishes the selection operation.
     */
    private void cancelSelection()
    {
        adapter.clearSelection();
        view.setDragEnabled(true);
        activeMode = new NormalMode();

        if (selectionListener != null) selectionListener.onSelectionFinish();
    }

    /**
     * Selects or deselects the item at a given position
     *
     * @param position the position of the item to be selected/deselected
     */
    protected void toggleSelection(int position)
    {
        adapter.toggleSelection(position);
        activeMode = adapter.isSelectionEmpty() ? NORMAL_MODE : SELECTION_MODE;
    }

    public interface HabitListener extends CheckmarkButtonController.Listener
    {
        /**
         * Called when the user clicks a habit.
         *
         * @param habit the habit clicked
         */
        void onHabitClick(Habit habit);

        /**
         * Called when the user wants to change the position of a habit on the
         * list.
         *
         * @param from habit to be moved
         * @param to   habit that currently occupies the desired position
         */
        void onHabitReorder(Habit from, Habit to);
    }

    /**
     * A Mode describes the behaviour of the list upon clicking, long clicking
     * and dragging an item. This depends on whether some items are already
     * selected or not.
     */
    private interface Mode
    {
        void onItemClick(int position);

        boolean onItemLongClick(int position);

        void startDrag(int position);
    }

    public interface SelectionListener
    {
        /**
         * Called when the user changes the list of selected item. This is only
         * called if there were previously selected items. If the selection was
         * previously empty, then onHabitSelectionStart is called instead.
         */
        void onSelectionChange();

        /**
         * Called when the user deselects all items or cancels the selection.
         */
        void onSelectionFinish();

        /**
         * Called after the user selects the first item.
         */
        void onSelectionStart();
    }

    /**
     * Mode activated when there are no items selected. Clicks trigger habit
     * click. Long clicks start selection.
     */
    class NormalMode implements Mode
    {
        @Override
        public void onItemClick(int position)
        {
            Habit habit = adapter.getItem(position);
            if (habitListener != null) habitListener.onHabitClick(habit);
        }

        @Override
        public boolean onItemLongClick(int position)
        {
            startSelection(position);
            return true;
        }

        @Override
        public void startDrag(int position)
        {
            startSelection(position);
        }

        protected void startSelection(int position)
        {
            toggleSelection(position);
            activeMode = SELECTION_MODE;
            if (selectionListener != null) selectionListener.onSelectionStart();
        }
    }

    /**
     * Mode activated when some items are already selected.
     * <p>
     * Clicks toggle item selection. Long clicks select more items.
     */
    class SelectionMode implements Mode
    {
        @Override
        public void onItemClick(int position)
        {
            toggleSelection(position);
            notifyListener();
        }

        @Override
        public boolean onItemLongClick(int position)
        {
            toggleSelection(position);
            notifyListener();
            return true;
        }

        @Override
        public void startDrag(int position)
        {
            toggleSelection(position);
            notifyListener();
        }

        protected void notifyListener()
        {
            if(selectionListener == null) return;

            if (activeMode == SELECTION_MODE)
                selectionListener.onSelectionChange();
            else
                selectionListener.onSelectionFinish();
        }
    }
}
