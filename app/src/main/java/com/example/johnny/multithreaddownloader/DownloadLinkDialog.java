package com.example.johnny.multithreaddownloader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * Created by Johnny on 2016/10/22.
 */

public class DownloadLinkDialog extends DialogFragment {
    private EditText etDownloadLink;

    public interface DownloadLinkListener{
        void onDownloadLinkComplete(String downloadLink);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_download_link,null);
        etDownloadLink = (EditText) view.findViewById(R.id.et_download_link);
        builder.setView(view).setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DownloadLinkListener listener = (DownloadLinkListener) getActivity();
                listener.onDownloadLinkComplete(etDownloadLink.getText().toString());
            }
        }).setNegativeButton("取消",null);
        return builder.create();
    }
}
