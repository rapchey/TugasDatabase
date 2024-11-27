package com.example.tugasdatabase;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

import java.util.List;

public class HistoryAdapter extends ArrayAdapter<HistoryItem> {

    public HistoryAdapter(@NonNull Context context, @NonNull List<HistoryItem> objects) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_history, parent, false);
        }

        HistoryItem currentItem = getItem(position);

        ImageView albumImage = convertView.findViewById(R.id.historyAlbumImage);
        TextView songTitle = convertView.findViewById(R.id.historySongTitle);
        AppCompatImageButton deleteButton = convertView.findViewById(R.id.deleteHistoryButton);

        if (currentItem != null) {
            albumImage.setImageResource(currentItem.getAlbumImageResId());
            songTitle.setText(currentItem.getTitle());

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    remove(currentItem);

                    SQLiteDatabase database = new SQLiteHelper(getContext()).getWritableDatabase();
                    database.execSQL("DELETE FROM " + SQLiteHelper.TABLE_HISTORY + " WHERE " +
                            SQLiteHelper.COLUMN_TITLE + " = ?", new String[]{currentItem.getTitle()});

                    notifyDataSetChanged();
                }
            });
        }

        return convertView;
    }
}