package io.branch.referral.validators;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.Defines;

public class DeepLinkRoutingValidator {
    private static final String VALIDATE_LINK_PARAM_KEY = "validate";
    private static final String BRANCH_VALIDATE_TEST_KEY = "_branch_validate";
    private static final int BRANCH_VALIDATE_TEST_VALUE = 60514;
    private static final String URI_REDIRECT_KEY = "$uri_redirect_mode=";
    private static final String URI_REDIRECT_MODE = "2";
    private static final int LAUNCH_TEST_TEMPLATE_DELAY = 500; // .5 sec delay to settle any auto deep linking

    public static void validate(final Activity activity) {
        String latestReferringLink = getLatestReferringLink();
        if (!TextUtils.isEmpty(latestReferringLink)) {
            final JSONObject response_data = Branch.getInstance().getLatestReferringParams();
            if (response_data.optInt(BRANCH_VALIDATE_TEST_KEY) == BRANCH_VALIDATE_TEST_VALUE) {
                if (response_data.optBoolean(Defines.Jsonkey.Clicked_Branch_Link.getKey())) {
                    validateDeeplinkRouting(activity, response_data);
                } else {
                    displayErrorMessage(activity);
                }
            } else if (!response_data.optBoolean(VALIDATE_LINK_PARAM_KEY)) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        launchTestTemplate(activity, getUpdatedLinkWithTestStat(response_data, ""));
                    }
                }, LAUNCH_TEST_TEMPLATE_DELAY);
            }
        }
    }


    private static void validateDeeplinkRouting(final Activity context, final JSONObject validate_json) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(context);
        }
        builder.setTitle("Branch Deeplinking Routing")
                .setMessage("Good news - we got link data. Now a question for you, astute developer: did the app deep link to the specific piece of content you expected to see?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Test Succeeded
                        String launch_link = getUpdatedLinkWithTestStat(validate_json, "g");
                        launchTestTemplate(context, launch_link);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Test Failed
                        String launch_link = getUpdatedLinkWithTestStat(validate_json, "r");
                        launchTestTemplate(context, launch_link);
                    }
                })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setCancelable(false)
                .setIcon(android.R.drawable.sym_def_app_icon)
                .show();
    }

    private static void launchTestTemplate(final Context context, String url) {
        Uri launchUri = Uri.parse(url)
                .buildUpon()
                .appendQueryParameter(URI_REDIRECT_KEY,URI_REDIRECT_MODE)
                .build();
        // Appending URI redirect mode to the the URL
        Intent i = new Intent(Intent.ACTION_VIEW, launchUri);
        //i.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setPackage("com.android.chrome");
        context.getPackageManager().queryIntentActivities(i, 0);
        try {

            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            // Chrome is probably not installed
            // Try with the default browser
            i.setPackage(null);
            context.startActivity(i);
        }
    }

    private static String getUpdatedLinkWithTestStat(JSONObject blob, String result) {
        String link = "";
        try {
            link = blob.getString("~" + Defines.Jsonkey.ReferringLink.getKey());
            link = link.split("\\?")[0];
        } catch (Exception e) {
            Log.e("BRANCH SDK", "Failed to get referring link");
        }
        link += "?" + VALIDATE_LINK_PARAM_KEY + "=true";
        link += "&" + URI_REDIRECT_KEY + "=" + URI_REDIRECT_MODE;
        if (!TextUtils.isEmpty(result)) {
            try {
                link += blob.getString("ct").equals("t1") ? "&t1=" + result : "&t1=" + blob.getString("t1");
                link += blob.getString("ct").equals("t2") ? "&t2=" + result : "&t2=" + blob.getString("t2");
                link += blob.getString("ct").equals("t3") ? "&t3=" + result : "&t3=" + blob.getString("t3");
                link += blob.getString("ct").equals("t4") ? "&t4=" + result : "&t4=" + blob.getString("t4");
                link += blob.getString("ct").equals("t5") ? "&t5=" + result : "&t5=" + blob.getString("t5");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        link += "&os=android";
        return link;
    }

    private static void displayErrorMessage(final Activity context) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(context);
        }
        builder.setTitle("Branch Deeplink Routing Support")
                .setMessage("Bummer. It seems like +clicked_branch_link is false - we didn't deep link.  Double check that the link you're clicking has the same branch_key that is being used in your Manifest file. Return to Chrome when you're ready to test again.")
                .setNeutralButton("Got it", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setCancelable(false)
                .setIcon(android.R.drawable.sym_def_app_icon)
                .show();
    }

    private static String getLatestReferringLink() {
        String latestReferringLink = "";
        if (Branch.getInstance() != null && Branch.getInstance().getLatestReferringParams() != null) {
            latestReferringLink = Branch.getInstance().getLatestReferringParams().optString("~" + Defines.Jsonkey.ReferringLink.getKey());
        }
        return latestReferringLink;
    }

}
