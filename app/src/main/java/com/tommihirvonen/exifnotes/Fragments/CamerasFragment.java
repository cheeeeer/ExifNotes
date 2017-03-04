package com.tommihirvonen.exifnotes.Fragments;

// Copyright 2015
// Tommi Hirvonen

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tommihirvonen.exifnotes.Adapters.CameraAdapter;
import com.tommihirvonen.exifnotes.Datastructures.Camera;
import com.tommihirvonen.exifnotes.Datastructures.Lens;
import com.tommihirvonen.exifnotes.Dialogs.EditCameraInfoDialog;
import com.tommihirvonen.exifnotes.Utilities.FilmDbHelper;
import com.tommihirvonen.exifnotes.Activities.GearActivity;
import com.tommihirvonen.exifnotes.R;
import com.tommihirvonen.exifnotes.Utilities.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CamerasFragment extends Fragment implements
        View.OnClickListener,
        AdapterView.OnItemClickListener {

    public static final int ADD_CAMERA = 1;
    public static final int EDIT_CAMERA = 2;
    TextView mainTextView;
    ListView mainListView;
    CameraAdapter cameraAdapter;
    List<Camera> cameraList;
    FilmDbHelper database;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LayoutInflater linf = getActivity().getLayoutInflater();

        database = FilmDbHelper.getInstance(getActivity());
        cameraList = database.getAllCameras();

        final View view = linf.inflate(R.layout.cameras_fragment, container, false);

        FloatingActionButton floatingActionButton = (FloatingActionButton) view.findViewById(R.id.fab_cameras);
        floatingActionButton.setOnClickListener(this);

        int secondaryColor = Utilities.getSecondaryUiColor(getActivity());

        // Also change the floating action button color. Use the darker secondaryColor for this.
        floatingActionButton.setBackgroundTintList(ColorStateList.valueOf(secondaryColor));

        mainTextView = (TextView) view.findViewById(R.id.no_added_cameras);

        // Access the ListView
        mainListView = (ListView) view.findViewById(R.id.main_cameraslistview);

        // Create an ArrayAdapter for the ListView
        cameraAdapter = new CameraAdapter(
                getActivity(), android.R.layout.simple_list_item_1, cameraList);

        // Set the ListView to use the ArrayAdapter
        mainListView.setAdapter(cameraAdapter);

        // Set this activity to react to list items being pressed
        mainListView.setOnItemClickListener(this);

        registerForContextMenu(mainListView);

        // Color the item dividers of the ListView
        int[] dividerColors = {0, R.color.grey, 0};
        mainListView.setDivider(
                new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, dividerColors));
        mainListView.setDividerHeight(2);

        if (cameraList.size() >= 1) mainTextView.setVisibility(View.GONE);

        cameraAdapter.notifyDataSetChanged();

        return view;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_context_delete_edit_select_lenses, menu);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        view.performLongClick();
    }

    @SuppressLint("CommitTransaction")
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        // Because of a bug with ViewPager and context menu actions,
        // we have to check which fragment is visible to the user.
        if (getUserVisibleHint()) {

            int which = info.position;
            Camera camera = cameraList.get(which);

            switch (item.getItemId()) {

                case R.id.menu_item_select_mountable_lenses:

                    showSelectMountableLensesDialog(which);
                    return true;

                case R.id.menu_item_delete:

                    // Check if the camera is being used with one of the rolls.
                    if (database.isCameraBeingUsed(camera)) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.CameraNoColon) +
                                " " + camera.getMake() + " " + camera.getModel() + " " +
                                getResources().getString(R.string.IsBeingUsed), Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    database.deleteCamera(camera);

                    // Remove the roll from the lensList. Do this last!!!
                    cameraList.remove(which);

                    if (cameraList.size() == 0) mainTextView.setVisibility(View.VISIBLE);
                    cameraAdapter.notifyDataSetChanged();

                    // Update the LensesFragment through the parent activity.
                    GearActivity myActivity = (GearActivity)getActivity();
                    myActivity.updateFragments();

                    return true;

                case R.id.menu_item_edit:

                    EditCameraInfoDialog dialog = new EditCameraInfoDialog();
                    dialog.setTargetFragment(this, EDIT_CAMERA);
                    Bundle arguments = new Bundle();
                    arguments.putString("TITLE", getResources().getString( R.string.EditCamera));
                    arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.OK));
                    arguments.putParcelable("CAMERA", camera);

                    dialog.setArguments(arguments);
                    dialog.show(getFragmentManager().beginTransaction(), EditCameraInfoDialog.TAG);

                    return true;
            }
        }
        return false;
    }

    @SuppressLint("CommitTransaction")
    public void showCameraNameDialog() {
        EditCameraInfoDialog dialog = new EditCameraInfoDialog();
        dialog.setTargetFragment(this, ADD_CAMERA);
        Bundle arguments = new Bundle();
        arguments.putString("TITLE", getResources().getString( R.string.NewCamera));
        arguments.putString("POSITIVE_BUTTON", getResources().getString(R.string.Add));
        dialog.setArguments(arguments);
        dialog.show(getFragmentManager().beginTransaction(), EditCameraInfoDialog.TAG);
    }

    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fab_cameras:
                showCameraNameDialog();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {

            case ADD_CAMERA:

                if (resultCode == Activity.RESULT_OK) {
                    // After Ok code.

                    Camera camera = data.getParcelableExtra("CAMERA");

                    if (camera.getMake().length() > 0 && camera.getModel().length() > 0) {

                        mainTextView.setVisibility(View.GONE);

                        long rowId = database.addCamera(camera);
                        camera.setId(rowId);
                        cameraList.add(camera);
                        cameraAdapter.notifyDataSetChanged();

                        // When the lens is added jump to view the last entry
                        mainListView.setSelection(mainListView.getCount() - 1);
                    }

                } else if (resultCode == Activity.RESULT_CANCELED){
                    // After Cancel code.
                    // Do nothing.
                    return;
                }
                break;

            case EDIT_CAMERA:

                if (resultCode == Activity.RESULT_OK) {

                    Camera camera = data.getParcelableExtra("CAMERA");

                    if (camera.getMake().length() > 0 &&
                            camera.getModel().length() > 0 &&
                            camera.getId() > 0) {

                        database.updateCamera(camera);

                        cameraAdapter.notifyDataSetChanged();
                        // Update the LensesFragment through the parent activity.
                        GearActivity gearActivity = (GearActivity)getActivity();
                        gearActivity.updateFragments();

                    } else {
                        Toast.makeText(getActivity(), "Something went wrong :(",
                                Toast.LENGTH_SHORT).show();
                    }

                } else if (resultCode == Activity.RESULT_CANCELED){

                    return;
                }

                break;
        }
    }

    void showSelectMountableLensesDialog(int position){
        final Camera camera = cameraList.get(position);
        final List<Lens> mountableLenses = database.getMountableLenses(camera);
        final List<Lens> allLenses = database.getAllLenses();

        // Make a list of strings for all the camera names to be showed in the
        // multi choice list.
        // Also make an array list containing all the camera id's for list comparison.
        // Comparing lists containing frames is not easy.
        List<String> listItems = new ArrayList<>();
        List<Long> allLensesId = new ArrayList<>();
        for (int i = 0; i < allLenses.size(); ++i) {
            listItems.add(allLenses.get(i).getMake() + " " + allLenses.get(i).getModel());
            allLensesId.add(allLenses.get(i).getId());
        }

        // Make an array list containing all mountable camera id's.
        List<Long> mountableLensesId = new ArrayList<>();
        for (int i = 0; i < mountableLenses.size(); ++i) {
            mountableLensesId.add(mountableLenses.get(i).getId());
        }

        // Find the items in the list to be preselected
        final boolean[] booleans = new boolean[allLenses.size()];
        for (int i= 0; i < allLensesId.size(); ++i) {
            booleans[i] = mountableLensesId.contains(allLensesId.get(i));
        }

        final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // MULTIPLE CHOICE DIALOG

        // Create an array list where the selections are saved. Initialize it with
        // the booleans array.
        final List<Integer> selectedItemsIndexList = new ArrayList<>();
        for (int i = 0; i < booleans.length; ++i) {
            if (booleans[i]) selectedItemsIndexList.add(i);
        }

        builder.setTitle(R.string.SelectMountableLenses)
                .setMultiChoiceItems(items, booleans, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {

                            // If the user checked the item, add it to the selected items
                            selectedItemsIndexList.add(which);

                        } else if (selectedItemsIndexList.contains(which)) {

                            // Else, if the item is already in the array, remove it
                            selectedItemsIndexList.remove(Integer.valueOf(which));

                        }
                    }
                })

                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        // Do something with the selections
                        Collections.sort(selectedItemsIndexList);

                        // Get the not selected indices.
                        ArrayList<Integer> notSelectedItemsIndexList = new ArrayList<>();
                        for (int i = 0; i < allLenses.size(); ++i) {
                            if (!selectedItemsIndexList.contains(i))
                                notSelectedItemsIndexList.add(i);
                        }

                        // Iterate through the selected items
                        for (int i = selectedItemsIndexList.size() - 1; i >= 0; --i) {
                            int which = selectedItemsIndexList.get(i);
                            Lens lens = allLenses.get(which);
                            database.addMountable(camera, lens);
                        }

                        // Iterate through the not selected items
                        for (int i = notSelectedItemsIndexList.size() - 1; i >= 0; --i) {
                            int which = notSelectedItemsIndexList.get(i);
                            Lens lens = allLenses.get(which);
                            database.deleteMountable(camera, lens);
                        }
                        cameraAdapter.notifyDataSetChanged();

                        // Update the LensesFragment through the parent activity.
                        GearActivity gearActivity = (GearActivity)getActivity();
                        gearActivity.updateFragments();
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void updateFragment(){
        cameraAdapter.notifyDataSetChanged();
    }
}
