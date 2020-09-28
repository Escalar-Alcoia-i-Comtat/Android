package com.arnyminerz.escalaralcoiaicomtat.view.train

import android.app.Activity
import android.widget.LinearLayout
import com.arnyminerz.escalaralcoiaicomtat.data.train.TrainDataHolder

abstract class TrainElement(
    protected val activity: Activity,
    protected val parentLayout: LinearLayout,
    var data: TrainDataHolder
) {
    var index: Int = 0
        protected set
}