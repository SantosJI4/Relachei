package com.noveflix.app.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * VideoView que sempre preenche o container ignorando a proporção do vídeo.
 * Corrige o bug onde vídeos 16:9 ficavam com altura reduzida num container 9:16.
 */
public class FullscreenVideoView extends VideoView {

    public FullscreenVideoView(Context context) {
        super(context);
    }

    public FullscreenVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FullscreenVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Força o VideoView a preencher todo o espaço do container
        // independentemente da proporção do vídeo
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        );
    }
}
