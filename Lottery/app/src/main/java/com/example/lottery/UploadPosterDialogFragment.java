package com.example.lottery;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * DialogFragment for handling event poster selection and preview.
 * Provides functionality to browse the device, preview selected images, and enlarge them.
 */
public class UploadPosterDialogFragment extends DialogFragment {

    private Uri tempUri = null;
    private ImageView ivPreview;
    private Button btnSelect;
    private View cvPreviewContainer;
    private View tvHint;
    private ActivityResultLauncher<String> pickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize the image picker launcher
        pickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                tempUri = uri;
                updatePreviewLayout(uri);
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_upload_poster, null);

        ivPreview = view.findViewById(R.id.ivDialogPreview);
        btnSelect = view.findViewById(R.id.btnSelectConfirm);
        Button btnBrowse = view.findViewById(R.id.btnBrowseImage);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        cvPreviewContainer = view.findViewById(R.id.cvDialogPreview);
        tvHint = view.findViewById(R.id.tvPreviewHint);

        // Browse button click listener
        btnBrowse.setOnClickListener(v -> pickerLauncher.launch("image/*"));

        // Cancel button click listener
        btnCancel.setOnClickListener(v -> dismiss());

        // Confirm the selection and pass it back to the Activity
        btnSelect.setOnClickListener(v -> {
            if (tempUri != null) {
                Bundle result = new Bundle();
                result.putString("posterUri", tempUri.toString());
                getParentFragmentManager().setFragmentResult("posterRequest", result);
                dismiss();
            }
        });

        /**
         * US 02.04.01 Upgrade: Enlarging preview
         * Clicking the thumbnail container opens a larger view.
         */
        cvPreviewContainer.setOnClickListener(v -> {
            if (tempUri != null) {
                showEnlargedPreview(tempUri);
            }
        });

        builder.setView(view);
        return builder.create();
    }

    /**
     * Updates the dialog UI to show the selected image thumbnail and enable confirmation.
     */
    private void updatePreviewLayout(Uri uri) {
        ivPreview.setImageURI(uri);
        // Enable visual feedback and selection button
        tvHint.setVisibility(View.VISIBLE);
        btnSelect.setEnabled(true);
    }

    /**
     * Displays a simple alert dialog with a larger version of the selected image.
     * Fulfills detail requirements for US 02.04.01.
     */
    private void showEnlargedPreview(Uri uri) {
        ImageView enlargedIv = new ImageView(requireContext());
        enlargedIv.setImageURI(uri);
        enlargedIv.setAdjustViewBounds(true);
        enlargedIv.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(requireContext())
                .setTitle("Poster Preview")
                .setView(enlargedIv)
                .setPositiveButton("Close", null)
                .show();
    }
}
