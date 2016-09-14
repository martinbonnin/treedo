package com.mbonnin.treedo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.example.android.trivialdrivesample.util.IabException;
import com.example.android.trivialdrivesample.util.IabHelper;
import com.example.android.trivialdrivesample.util.Inventory;

import org.androidannotations.annotations.EBean;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;

import rx.Observer;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.util.async.Async;

/**
 * Created by martin on 9/14/16.
 */

@EBean(scope = EBean.Scope.Singleton)
public class InAppBilling {
    private final Scheduler mScheduler;
    private IabHelper mHelper;
    private static final String TAG = "InAppBilling";
    public static final String SKU_BEER = "beer_";
    public static final String SKU_GROWLER = "growler_";
    public static final String SKU_KEG = "keg_";

    static class MyExecutor implements Executor, Runnable {
        Queue<Runnable> mQueue = new LinkedList<>();

        public MyExecutor() {
            new Thread(this).start();
        }

        @Override
        public void execute(Runnable command) {
            synchronized (mQueue) {
                mQueue.offer(command);
                mQueue.notifyAll();
            }
        }

        @Override
        public void run() {
            while (true) {
                Runnable r = null;
                synchronized (mQueue) {
                    if (mQueue.isEmpty()) {
                        try {
                            mQueue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        r = mQueue.poll();
                    }
                }

                if (r != null) {
                    r.run();
                }
            }
        }
    }

    public InAppBilling(Context context) {
        /**
         * a stupid scheduler to serialize all calls to the IAB things
         */
        mScheduler = Schedulers.from(new MyExecutor());

        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjOQf9scJlfaEtp79eWVyQdLw6bqFiIXHvAjXvY+YdKpzvQFwSC0N72KgbAX9u6K5B2btAEziO3PPouubgN3426Ay00DWdjNUiGrpBhQ1Wzq4/+ZkwhPM3LKp6aH3y8B6oalnY9BeKWIBvzkPOxzuqoqghm2WSO/Al3K7bpcGQZkcHQO8g66e0PmTYOR3v5z7hIk16fqD77Raac8DhfY+0903e4ePsjok09OjJOJ7v3vgjCYpwnwn/apcCysEhSZk9fRw05R5du0VDWLFEHWYyCeTnV5c5Zx5JgWddbLnsb3Mnwlki14bpdUzYV2WgxcUvySJIFtNMF9m8t5nCzdoOwIDAQAB";
        mHelper = new IabHelper(context, base64EncodedPublicKey);
        mHelper.startSetup(result -> {
            if (!result.isSuccess()) {
                // Oh no, there was a problem.
                Log.d(TAG, "Problem setting up In-app Billing: " + result);
            } else {
                Log.d(TAG, "Hooray, IAB is fully set up ");
            }
        });
    }

    public IabHelper getHelper() {
        return mHelper;
    }

    public Scheduler getScheduler() {
        return mScheduler;
    }

    public void displayDialog(Context context) {
        BeerView view = BeerView_.build(context);

        Dialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        view.setDialog(dialog);
        dialog.show();
    }

    public void release() {
        if (mHelper != null) {
            try {
                mHelper.dispose();
            } catch (IabHelper.IabAsyncInProgressException e) {
                e.printStackTrace();
            }
        }
        mHelper = null;
    }
}
