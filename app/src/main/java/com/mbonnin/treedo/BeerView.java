package com.mbonnin.treedo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.example.android.trivialdrivesample.util.IabException;
import com.example.android.trivialdrivesample.util.IabHelper;
import com.example.android.trivialdrivesample.util.Inventory;
import com.example.android.trivialdrivesample.util.Purchase;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.util.async.Async;

import static com.mbonnin.treedo.InAppBilling.SKU_BEER;

@EViewGroup(R.layout.beer_view)
public class BeerView extends LinearLayout {
    public static final String TAG = "BeerView";

    @Bean
    InAppBilling mInAppBilling;

    @ViewById
    AppCompatButton beer;
    @ViewById
    AppCompatButton growler;
    @ViewById
    AppCompatButton keg;
    @ViewById
    ProgressBar progressBar;
    @ViewById
    View buttons;


    private Dialog mDialog;

    ArrayList<AppCompatButton> mButtonList = new ArrayList<>();
    ArrayList<String> mSkuList = new ArrayList<>();
    ArrayList<Integer> mTextList = new ArrayList<>();

    public BeerView(Context context) {
        super(context);
    }

    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = (result, purchase) -> {
        if (result.isFailure()) {
            Log.d(TAG, "cannot purchase");
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.thank_you)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();

        try {
            mInAppBilling.getHelper().consumeAsync(purchase, null);
        } catch (IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
        }
    };

    Observer<Inventory> mObserver = new Observer<Inventory>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
        }

        @Override
        public void onNext(Inventory inventory) {
            if (inventory == null) {
                return;
            }
            int states[][] = {{android.R.attr.state_pressed}, {}};
            int mutedColors[] = {getContext().getResources().getColor(R.color.muted_700), getContext().getResources().getColor(R.color.muted_500)};
            ColorStateList mutedColorStateList =  new ColorStateList(states, mutedColors);

            List<Purchase> purchases = inventory.getAllPurchases();
            if (purchases.size() > 0) {
                try {
                    mInAppBilling.getHelper().consumeAsync(purchases, null);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }
            }

            for (int i = 0; i < mSkuList.size(); i++) {
                String sku = mSkuList.get(i);
                mButtonList.get(i).setText(getContext().getText(mTextList.get(i)) + " - " + inventory.getSkuDetails(sku).getPrice());
                mButtonList.get(i).setSupportBackgroundTintList(mutedColorStateList);
                mButtonList.get(i).setOnClickListener(v->{
                    mDialog.dismiss();
                    try {
                        Log.d(TAG, "launchPurchaseFlow");
                        mInAppBilling.getHelper().launchPurchaseFlow(MainActivity.get(), sku, MainActivity.ACTIVITY_RESULT_IAB_FINISHED, mPurchaseFinishedListener);
                    } catch (IabHelper.IabAsyncInProgressException e) {
                        e.printStackTrace();
                    }
                });
            }

            buttons.setVisibility(VISIBLE);
            progressBar.setVisibility(GONE);

        }
    };

    public void setDialog(Dialog dialog) {
        mDialog = dialog;

        mButtonList.add(beer);
        mButtonList.add(growler);
        mButtonList.add(keg);
        mSkuList.add(SKU_BEER);
        mSkuList.add(InAppBilling.SKU_GROWLER);
        mSkuList.add(InAppBilling.SKU_KEG);
        mTextList.add(R.string.buy_beer);
        mTextList.add(R.string.buy_growler);
        mTextList.add(R.string.buy_keg);

        buttons.setVisibility(GONE);
        progressBar.setVisibility(VISIBLE);
        Async.start(() -> {
            try {
                return mInAppBilling.getHelper().queryInventory(true, mSkuList, null);
            } catch (IabException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }).subscribeOn(mInAppBilling.getScheduler())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mObserver);

    }

    @AfterViews
    void afterViews() {
        setOrientation(VERTICAL);
        int px = (int) Utils.toPixels(30);
        setPadding(px, px, px, px);
    }
}
