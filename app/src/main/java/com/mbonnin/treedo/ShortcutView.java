package com.mbonnin.treedo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.design.widget.Snackbar;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.EditText;
import android.widget.RelativeLayout;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

/**
 * Created by martin on 9/15/16.
 */
@EViewGroup(R.layout.shortcut_view)
public class ShortcutView extends RelativeLayout {

    @ViewById
    AppCompatButton button;
    @ViewById
    EditText editText;
    @ViewById
    RecyclerView recyclerView;

    private ShortcutAdapter mAdapter;
    private AlertDialog mDialog;
    private String mId;

    public ShortcutView(Context context) {
        super(context);
    }

    @AfterViews
    void afterViews() {
        int px = (int)Utils.toPixels(20);
        setPadding(px, px, px, px);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        mAdapter = new ShortcutAdapter(getContext());
        recyclerView.setAdapter(mAdapter);

        button.setOnClickListener(v -> {

            Paint whitePaint = new Paint();
            whitePaint.setStyle(Paint.Style.FILL);
            whitePaint.setColor(Color.WHITE);

            Drawable drawable = getContext().getResources().getDrawable(R.drawable.ic_treedo_checked);
            DrawableCompat.setTint(drawable, mAdapter.getSelectedColor());
            Bitmap bitmap = Bitmap.createBitmap(128,
                    128, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            canvas.drawRect(0.1f * canvas.getWidth(), 0.1f * canvas.getHeight(), 0.9f * canvas.getWidth(), 0.9f * canvas.getHeight(), whitePaint);
            drawable.draw(canvas);
            if (editText.getText().toString().equals("")) {
                Snackbar.make(this, getContext().getString(R.string.please_enter_a_name), Snackbar.LENGTH_SHORT).show();
                return;
            }

            Intent shortcutIntent = new Intent(getContext(), MainActivity_.class);
            shortcutIntent.putExtra(MainActivity.EXTRA_ID, mId);
            shortcutIntent.setAction(Intent.ACTION_MAIN);

            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, editText.getText().toString());
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);

            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            addIntent.putExtra("duplicate", false);
            getContext().sendBroadcast(addIntent);

            MainActivity.snackBarG(getContext().getString(R.string.shortcut_installed));

            if (mDialog != null) {
                mDialog.dismiss();
            }
        });
    }

    public void setDialog(AlertDialog dialog, String text, String id) {
        mDialog = dialog;
        mId = id;

        editText.setText(text);
    }

}
