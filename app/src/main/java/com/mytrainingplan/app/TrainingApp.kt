package com.mytrainingplan.app

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import dagger.hilt.android.HiltAndroidApp

/**
 * App Hilt (Paso 10 spec 006).
 *
 * Spec 012, paso 6: decodificador GIF registrado una vez para toda la app.
 * Todos los `AsyncImage` (ficha, lista, editor) reutilizan este `ImageLoader`
 * singleton; con `coil-gif` en el classpath Coil detecta y anima los GIF.
 * Sin red usa la caché de Coil; sin caché el `error` de cada `AsyncImage`
 * pinta su bloque de color sin caerse.
 */
@HiltAndroidApp
class TrainingApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
}
