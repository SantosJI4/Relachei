package com.noveflix.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Activity que exibe um anúncio via WebView e retorna o resultado.
 *
 * Extras de entrada:
 *   AD_URL  — URL completa da página de anúncio (/ad ou /ad/coins)
 *   AD_TYPE — "episode" ou "coins"
 *
 * Resultado (setResult):
 *   RESULT_OK   — anúncio assistido até o fim
 *   RESULT_CANCELED — usuário voltou sem assistir
 */
public class AdWebViewActivity extends Activity {

    public static final String EXTRA_AD_URL  = "AD_URL";
    public static final String EXTRA_AD_TYPE = "AD_TYPE";

    public static final int REQUEST_AD_EPISODE = 301;
    public static final int REQUEST_AD_COINS   = 302;

    private WebView webView;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Interface JS → Java: botão "Continuar" chama Android.onAdComplete()
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onAdComplete() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        setResult(RESULT_OK);
                        finish();
                    }
                });
            }
        }, "Android");

        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                // Intercepta o scheme noveflix:// vindo do botão
                if (url.startsWith("noveflix://")) {
                    setResult(RESULT_OK);
                    finish();
                    return true;
                }
                return false;
            }
        });

        String adUrl = getIntent().getStringExtra(EXTRA_AD_URL);
        if (adUrl != null) {
            webView.loadUrl(adUrl);
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
