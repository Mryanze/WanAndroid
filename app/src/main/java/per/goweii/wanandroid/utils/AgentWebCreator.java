package per.goweii.wanandroid.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.just.agentwebX5.AgentWebX5;
import com.just.agentwebX5.DefaultWebClient;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.CookieSyncManager;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import per.goweii.basic.utils.LogUtils;
import per.goweii.basic.utils.ResUtils;
import per.goweii.wanandroid.R;
import per.goweii.wanandroid.common.WanApp;
import per.goweii.wanandroid.widget.WebContainer;

/**
 * @author CuiZhen
 * @date 2019/8/31
 * QQ: 302833254
 * E-mail: goweii@163.com
 * GitHub: https://github.com/goweii
 */
public class AgentWebCreator {

    public static AgentWebX5 create(Activity activity,
                                    WebContainer container,
                                    String url,
                                    final ClientCallback clientCallback) {
        activity.getWindow().setFormat(PixelFormat.TRANSLUCENT);
        AgentWebX5 agentWeb = AgentWebX5.with(activity)
                .setAgentWebParent(container, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                .useDefaultIndicator()
                .setIndicatorColorWithHeight(ResUtils.getColor(activity, R.color.assist), 1)
                .interceptUnkownScheme()
                .setSecutityType(AgentWebX5.SecurityType.strict)
                .setOpenOtherPageWays(DefaultWebClient.OpenOtherPageWays.DISALLOW)
                .setWebChromeClient(new AgentWebX5ChromeClient(clientCallback))
                .setWebViewClient(new AgentWebX5ViewClient(clientCallback))
                //.setWebView(inflateWebView(activity))
                .createAgentWeb()
                .ready()
                .go(url);
        agentWeb.getWebCreator().get().setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        agentWeb.getWebCreator().get().getSettings().setJavaScriptEnabled(false);
        agentWeb.getWebCreator().get().getSettings().setBlockNetworkImage(false);
        agentWeb.getWebCreator().get().getSettings().setLoadsImagesAutomatically(true);
        agentWeb.getWebCreator().get().getSettings().setUseWideViewPort(true);
        agentWeb.getWebCreator().get().getSettings().setLoadWithOverviewMode(true);
        agentWeb.getWebCreator().get().getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(agentWeb.getWebCreator().get(), true);
        }
        return agentWeb;
    }

    public static AgentWebX5 create(Activity activity, WebContainer container, String url) {
        return create(activity, container, url, null);
    }

    private static WebView inflateWebView(Context context) {
        return (WebView) LayoutInflater.from(context).inflate(R.layout.layout_web_view, null);
    }

    private static void syncCookiesForWanAndroid(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        String host = Uri.parse(url).getHost();
        if (!TextUtils.equals(host, "www.wanandroid.com")) {
            return;
        }
        List<Cookie> cookies = WanApp.getCookieJar().loadForRequest(HttpUrl.get(url));
        if (cookies == null || cookies.isEmpty()) {
            return;
        }
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        for (Cookie cookie : cookies) {
            cookieManager.setCookie(url, cookie.name() + "=" + cookie.value());
        }
        cookieManager.flush();
        CookieSyncManager.createInstance(WanApp.getAppContext());
        CookieSyncManager.getInstance().sync();
    }


    public interface ClientCallback {
        void onReceivedUrl(String url);

        void onReceivedTitle(String title);

        void onHistoryUpdate(boolean isReload);

        void onPageStarted();

        void onProgressChanged(int progress);

        void onPageFinished();
    }

    public static class AgentWebX5ChromeClient extends WebChromeClient {
        private final ClientCallback mClientCallback;

        public AgentWebX5ChromeClient() {
            mClientCallback = null;
        }

        public AgentWebX5ChromeClient(ClientCallback clientCallback) {
            mClientCallback = clientCallback;
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (mClientCallback != null) {
                mClientCallback.onReceivedTitle(title);
            }
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (mClientCallback != null) {
                mClientCallback.onProgressChanged(newProgress);
            }
        }
    }

    public static class AgentWebX5ViewClient extends WebViewClient {
        private final ClientCallback mClientCallback;

        public AgentWebX5ViewClient() {
            mClientCallback = null;
        }

        public AgentWebX5ViewClient(ClientCallback clientCallback) {
            mClientCallback = clientCallback;
        }

        private boolean shouldInterceptRequest(Uri uri) {
            syncCookiesForWanAndroid(uri.toString());
            LogUtils.d("AgentWebX5Creator", "interceptUrlRequest:" + uri.toString());
            return false;
        }

        /**
         * true     拦截
         * false    加载
         */
        private boolean shouldOverrideUrlLoading(Uri uri) {
            LogUtils.d("AgentWebX5Creator", "overrideUrlLoading:" + uri.toString());
            switch (SettingUtils.getInstance().getUrlInterceptType()) {
                default:
                case HostInterceptUtils.TYPE_NOTHING:
                    return false;
                case HostInterceptUtils.TYPE_ONLY_WHITE:
                    return !HostInterceptUtils.isWhiteHost(uri.getHost());
                case HostInterceptUtils.TYPE_INTERCEPT_BLACK:
                    return HostInterceptUtils.isBlackHost(uri.getHost());
            }
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            if (shouldInterceptRequest(Uri.parse(url))) {
                return new WebResourceResponse(null, null, null);
            }
            return super.shouldInterceptRequest(view, url);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (shouldInterceptRequest(request.getUrl())) {
                return new WebResourceResponse(null, null, null);
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return shouldOverrideUrlLoading(Uri.parse(url));
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return shouldOverrideUrlLoading(request.getUrl());
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (mClientCallback != null) {
                mClientCallback.onReceivedUrl(url);
                mClientCallback.onReceivedTitle("");
                mClientCallback.onPageStarted();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (mClientCallback != null) {
                mClientCallback.onReceivedUrl(url);
                String title = view.getTitle();
                mClientCallback.onReceivedTitle(title == null ? "" : title);
                mClientCallback.onPageFinished();
            }
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            super.doUpdateVisitedHistory(view, url, isReload);
            if (mClientCallback != null) {
                mClientCallback.onHistoryUpdate(isReload);
            }
        }
    }

}
