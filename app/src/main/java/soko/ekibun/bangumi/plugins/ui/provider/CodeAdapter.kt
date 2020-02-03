package soko.ekibun.bangumi.plugins.ui.provider

import android.text.Editable
import android.text.TextWatcher
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_code_panel.view.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.provider.Provider
import java.lang.reflect.Field

class CodeAdapter(
    var provider: Provider,
    data: List<Field>?
) : BaseQuickAdapter<Field, BaseViewHolder>(R.layout.item_code_panel, data) {
    override fun convert(helper: BaseViewHolder, item: Field) {
        helper.itemView.item_label.text = item.getAnnotation(Provider.Code::class.java)?.label
        item.isAccessible = true
        helper.itemView.item_code.codeText.setText(provider.let { item.get(it) as? String })
        helper.itemView.item_code.codeText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                item.set(provider, s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { /* no-op */
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { /* no-op */
            }
        })
    }
}