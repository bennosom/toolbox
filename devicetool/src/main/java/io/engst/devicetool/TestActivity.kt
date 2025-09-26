package io.engst.devicetool

import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

class TestActivity : ComponentActivity() {

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      enableEdgeToEdge()
      window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
      window.setBackgroundDrawable(TRANSPARENT.toDrawable())

      setContent {
         Box(Modifier.fillMaxSize()) {
            val result = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie))
            if (result.value == null) {
               Text(result.error.toString())
            } else {
               LottieAnimation(
                  composition = result.value,
                  iterations = LottieConstants.IterateForever,
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
               )
            }
         }
      }
   }
}
