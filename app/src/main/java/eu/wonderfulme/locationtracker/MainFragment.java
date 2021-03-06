package eu.wonderfulme.locationtracker;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static eu.wonderfulme.locationtracker.LocationService.EXTRA_RECORD_PERIOD;


public class MainFragment extends Fragment {
    private static final String SAVE_STATE_IS_RECORDING = "SAVE_STATE_IS_RECORDING";
    private static final String SAVE_STATE_PERIOD_IN_SECOND = "SAVE_STATE_PERIOD_IN_SECOND";
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 110;

    private boolean mIsRecording;
    private int mPeriodInSeconds;
    @BindView(R.id.editText_record_period) protected EditText mRecordPeriodEditText;
    @BindView(R.id.fab_start_stop_record) protected FloatingActionButton mRecordFab;
    private RecordingButtonListener mRecordingButtonListener;
    @BindView(R.id.constraintLayout_main_fragment) protected ConstraintLayout mConstraintLayout;
    @BindView(R.id.adView_banner_main_fragment) protected AdView mAdView;

    public interface RecordingButtonListener {
        void onRecordingButtonClicked(boolean isRecording);
    }

    public MainFragment() { }

    public static MainFragment newInstance() { return new MainFragment(); }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, view);

        mIsRecording = false;

        // Init admob
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mRecordFab.setOnClickListener(new RecordFabClickListener());
        mPeriodInSeconds = Integer.parseInt(mRecordPeriodEditText.getText().toString());
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mIsRecording = savedInstanceState.getBoolean(SAVE_STATE_IS_RECORDING);
            mPeriodInSeconds = savedInstanceState.getInt(SAVE_STATE_PERIOD_IN_SECOND);
            // Restore FAB and editText
            if (mIsRecording) {
                mRecordPeriodEditText.setEnabled(false);
                mRecordPeriodEditText.setFocusable(false);
                mRecordingButtonListener.onRecordingButtonClicked(true);
                mRecordFab.setImageResource(android.R.drawable.ic_media_pause);
            }

        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVE_STATE_IS_RECORDING, mIsRecording);
        outState.putInt(SAVE_STATE_PERIOD_IN_SECOND, mPeriodInSeconds);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof RecordingButtonListener) {
            mRecordingButtonListener = (RecordingButtonListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement RecordingButtonListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mRecordingButtonListener = null;
    }

    private class RecordFabClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            boolean coarseLocationPermission = ContextCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED;
            boolean fineLocationPermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED;
            boolean writeStoragePermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED;

            if (!coarseLocationPermission || !fineLocationPermission || !writeStoragePermission) {
                ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_LOCATION);

            } else {
                handleClick();
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED && grantResults[1] == PERMISSION_GRANTED) {
                    handleClick();
                } else {
                    Snackbar.make(mConstraintLayout, getString(R.string.snackbar_location_permission_denied), Snackbar.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void handleClick() {
        if (!mIsRecording) {
            //Handle in case of start.
            startStopLocationService(true);
        } else {
            //Handle in case of stop.
            startStopLocationService(false);
        }
    }

    private void startStopLocationService(boolean start) {
        Intent intent = new Intent(getActivity(), LocationService.class);
        if (start) {
            mRecordPeriodEditText.setEnabled(false);
            mRecordPeriodEditText.setFocusable(false);
            mIsRecording = true;
            mRecordingButtonListener.onRecordingButtonClicked(true);
            mRecordFab.setImageResource(android.R.drawable.ic_media_pause);
            mPeriodInSeconds = Integer.parseInt(mRecordPeriodEditText.getText().toString());
            intent.putExtra(EXTRA_RECORD_PERIOD, mPeriodInSeconds);
            Objects.requireNonNull(getActivity()).startService(intent);
            Snackbar.make(mConstraintLayout, getString(R.string.snackbar_put_app_in_background), Snackbar.LENGTH_SHORT).show();
        } else {
            mRecordPeriodEditText.setEnabled(true);
            mRecordPeriodEditText.setFocusable(true);
            mIsRecording = false;
            mRecordingButtonListener.onRecordingButtonClicked(false);
            mRecordFab.setImageResource(android.R.drawable.ic_media_play);
            Objects.requireNonNull(getActivity()).stopService(intent);
            // Save all table content to a csv file and delete all db contents.
            Snackbar resultSnackbar = Snackbar.make(mConstraintLayout, "", Snackbar.LENGTH_SHORT);
            ExportAndCleanDbAsyncTask exportAndCleanDb = new ExportAndCleanDbAsyncTask(getActivity().getApplicationContext(), resultSnackbar);
            exportAndCleanDb.execute();
        }
    }

}
