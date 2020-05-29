package soko.ekibun.bangumi.plugins.engine

import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory

object RhinoContextFactory : ContextFactory() {
    override fun hasFeature(cx: Context?, featureIndex: Int): Boolean {
        if (featureIndex == Context.FEATURE_ENABLE_XML_SECURE_PARSING) return false
        return super.hasFeature(cx, featureIndex)
    }
}