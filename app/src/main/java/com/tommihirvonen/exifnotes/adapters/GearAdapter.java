package com.tommihirvonen.exifnotes.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.datastructures.Camera;
import com.tommihirvonen.exifnotes.datastructures.Filter;
import com.tommihirvonen.exifnotes.datastructures.Gear;
import com.tommihirvonen.exifnotes.datastructures.Lens;
import com.tommihirvonen.exifnotes.utilities.FilmDbHelper;

import java.util.Collections;
import java.util.List;

/**
 * GearAdapter acts as an adapter between a List of gear and a RecyclerView.
 */
public class GearAdapter extends RecyclerView.Adapter<GearAdapter.ViewHolder> {

    /**
     * Reference to the main list of gear received from implementing class.
     */
    private List<? extends Gear> gearList;

    /**
     * Reference to Activity's context. Used to get resources.
     */
    private final Context context;

    /**
     * Reference to the singleton database.
     */
    private final FilmDbHelper database;

    /**
     * Package-private ViewHolder class which can be recycled
     * for better performance and memory management.
     * All common view elements for all items are initialized here.
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout linearLayout;
        TextView nameTextView;
        TextView mountablesTextView;
        ViewHolder(View itemView) {
            super(itemView);
            linearLayout = itemView.findViewById(R.id.item_gear_layout);
            nameTextView = itemView.findViewById(R.id.tv_gear_name);
            mountablesTextView = itemView.findViewById(R.id.tv_mountables);
            // Instead of short click perform long click to activate the OnCreateContextMenuListener.
            linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    view.performLongClick();
                }
            });
            linearLayout.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
                    final Gear gear = gearList.get(getAdapterPosition());
                    final String gearName = gear.getName();
                    contextMenu.setHeaderTitle(gearName);
                    // If the piece of gear is a camera or a filter, add the same menu item.

                    // Use the order parameter (3rd parameter) of the ContextMenu.add() method
                    // to pass the position of the list item which was clicked.
                    // This can be used in the implementing class to retrieve the items position.
                    if (gear instanceof Camera || gear instanceof Filter) {
                        contextMenu.add(0, R.id.menu_item_select_mountable_lenses, getAdapterPosition(), R.string.SelectMountableLenses);
                    // If the piece of gear is a lens, add two menu items.
                    } else if (gear instanceof Lens) {
                        contextMenu.add(0, R.id.menu_item_select_mountable_cameras, getAdapterPosition(), R.string.SelectMountableCameras);
                        contextMenu.add(0, R.id.menu_item_select_mountable_filters, getAdapterPosition(), R.string.SelectMountableFilters);
                    }
                    // Add the additional menu items common for all types of gear.
                    contextMenu.add(0, R.id.menu_item_edit, getAdapterPosition(), R.string.Edit);
                    contextMenu.add(0, R.id.menu_item_delete, getAdapterPosition(), R.string.Delete);
                }
            });
        }
    }

    /**
     * Constructor for this adapter,
     *
     * @param context activity's context
     * @param gearList list of gear from implementing class
     */
    public GearAdapter(Context context, List<? extends Gear> gearList) {
        this.gearList = gearList;
        this.context = context;
        this.database = FilmDbHelper.getInstance(context);
    }

    /**
     * Invoked by LayoutManager to create new Views.
     *
     * @param parent view's parent ViewGroup
     * @param viewType not used
     * @return inflated view
     */
    @Override
    public GearAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gear, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Invoked by LayoutManager to replace the contents of a View.
     * Here we get the element from our dataset at the specified position
     * and set the ViewHolder views to display said elements data.
     *
     * @param holder reference to the recyclable ViewHolder
     * @param position position of the current item
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Gear gear = gearList.get(position);
        if (gear != null) {
            final String gearName = gear.getName();
            List<? extends Gear> mountables1 = Collections.emptyList();
            List<? extends Gear> mountables2 = Collections.emptyList();
            // If the type of gear is lens, then get both mountable types.
            if (gear instanceof Lens) {
                mountables1 = database.getMountableCameras((Lens) gear);
                mountables2 = database.getMountableFilters((Lens) gear);
            } else if (gear instanceof Camera) {
                mountables1 = database.getMountableLenses((Camera) gear);
            } else if (gear instanceof Filter) {
                mountables1 = database.getMountableLenses((Filter) gear);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(context.getString(R.string.MountsTo));
            for (Gear g : mountables1) {
                stringBuilder.append("\n- ").append(g.getName());
            }
            // If the second list of mountables is not empty, add a line change and additional mountables.
            if (!mountables2.isEmpty()) stringBuilder.append("\n");
            // For loop not iterated, if mountables2 is empty.
            for (Gear g : mountables2) {
                stringBuilder.append("\n- ").append(g.getName());
            }
            holder.nameTextView.setText(gearName);
            holder.mountablesTextView.setText(stringBuilder.toString());
        }
    }

    /**
     * Method to get the item count of the FrameAdapter.
     *
     * @return the size of the main frameList
     */
    @Override
    public int getItemCount() {
        return gearList.size();
    }

}