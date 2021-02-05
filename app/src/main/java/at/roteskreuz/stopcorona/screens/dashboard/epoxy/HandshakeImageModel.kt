package at.roteskreuz.stopcorona.screens.dashboard.epoxy

import android.widget.ImageView
import at.roteskreuz.stopcorona.R
import at.roteskreuz.stopcorona.screens.dashboard.HealthStatusData
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyHolder
import at.roteskreuz.stopcorona.skeleton.core.screens.base.view.BaseEpoxyModel
import at.roteskreuz.stopcorona.skeleton.core.utils.visible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.lottie.LottieAnimationView

/**
 * Model with one big animating handshake image if active or big static grayscale image if inactive.
 */
@EpoxyModelClass(layout = R.layout.dashboard_handshake_image_epoxy_model)
abstract class HandshakeImageModel : BaseEpoxyModel<HandshakeImageModel.Holder>() {

    @EpoxyAttribute
    var active: Boolean = false

    @EpoxyAttribute
    var data: HealthStatusData = HealthStatusData.NoHealthStatus

    override fun Holder.onBind() {
        val healthStatusData: HealthStatusData = data

        imgActive.visible = active
        imgInactive.visible = !active

        if (active){
            when(healthStatusData) {
                is HealthStatusData.SicknessCertificate -> imgActive.setAnimation(R.raw.handshake_animation_sickness)
                is HealthStatusData.SelfTestingSuspicionOfSickness -> imgActive.setAnimation(R.raw.handshake_animation_suspicion_sickness)
                is HealthStatusData.ContactsSicknessInfo -> {
                    if (healthStatusData.warningType.redContactsDetected){
                        imgActive.setAnimation(R.raw.handshake_animation_contacts_sickness_red)
                    } else {
                        imgActive.setAnimation(R.raw.handshake_animation_contacts_sickness_yellow)
                    }
                }
                else -> imgActive.setAnimation(R.raw.handshake_animation_blue)
            }
        }
    }

    override fun onViewAttachedToWindow(holder: Holder) {
        super.onViewAttachedToWindow(holder)
        with(holder) {
            if (active) {
                imgActive.playAnimation()
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: Holder) {
        with(holder) {
            if (imgActive.isAnimating) {
                imgActive.cancelAnimation()
                imgActive.progress = 0f
            }
        }
        super.onViewDetachedFromWindow(holder)
    }

    class Holder : BaseEpoxyHolder() {
        val imgActive by bind<LottieAnimationView>(R.id.imgActive)
        val imgInactive by bind<ImageView>(R.id.imgInactive)
    }
}