package soko.ekibun.bangumi.plugins.ui.provider

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.android.synthetic.main.activity_provider.*
import soko.ekibun.bangumi.plugins.R
import soko.ekibun.bangumi.plugins.model.LineProvider
import soko.ekibun.bangumi.plugins.provider.Provider
import soko.ekibun.bangumi.plugins.util.JsonUtil
import soko.ekibun.bangumi.plugins.util.ReflectUtil

class ProviderActivity : AppCompatActivity(), ColorPickerDialogListener {
    override fun onDialogDismissed(dialogId: Int) {
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        item_provider_color_hex.text = colorToString(color)
        item_provider_color_prev.backgroundTintList = ColorStateList.valueOf(color)
    }

    val color get() = Color.parseColor(item_provider_color_hex.text.toString())

    private fun colorToString(color: Int): String {
        return "#" + String.format("%08x", color).substring(2)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provider)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val listPaddingBottom = detail_container.paddingBottom
        root_layout.setOnApplyWindowInsetsListener { _, insets ->
            detail_container.setPadding(
                detail_container.paddingLeft,
                detail_container.paddingTop,
                detail_container.paddingRight,
                listPaddingBottom + insets.systemWindowInsetBottom
            )
            insets
        }

        item_codes.layoutManager = object : LinearLayoutManager(this) {
            override fun requestChildRectangleOnScreen(
                parent: RecyclerView,
                child: View,
                rect: Rect,
                immediate: Boolean
            ): Boolean {
                return false
            }

            override fun requestChildRectangleOnScreen(
                parent: RecyclerView,
                child: View,
                rect: Rect,
                immediate: Boolean,
                focusedChildVisible: Boolean
            ): Boolean {
                return false
            }
        }

        item_provider_color_hex.text = colorToString(0)
        setProvider(intent?.getStringExtra(EXTRA_PROVIDER_INFO))

        item_provider_color.setOnClickListener {
            ColorPickerDialog.newBuilder().setColor(color)
                .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                .setAllowPresets(false)
                .setDialogId(0)
                .show(this)
        }
    }

    var adapter: CodeAdapter? = null
    val type by lazy { intent.getStringExtra(EXTRA_PROVIDER_TYPE)!! }
    val typeClass by lazy { Provider.providers[type]!! }
    private fun setProvider(info: String?) {
        val provider = JsonUtil.toEntity<LineProvider.ProviderInfo>(info ?: "")
            ?: LineProvider.ProviderInfo("", 0 ,"", type)
        val providerCode = JsonUtil.toEntity(provider.code, typeClass) ?: ReflectUtil.newInstance(typeClass)
        adapter = adapter ?: CodeAdapter(providerCode, ReflectUtil.getAllFields(typeClass).filter {
            it.isAnnotationPresent(Provider.Code::class.java)
        }.sortedBy { it.getAnnotation(Provider.Code::class.java)!!.index })
        adapter?.provider = providerCode
        item_codes.adapter = adapter

        item_provider_site.setText(provider.site)
        item_provider_color_hex.text = colorToString(provider.color)
        item_provider_color_prev.backgroundTintList = ColorStateList.valueOf(color)
        item_provider_title.setText(provider.title)
    }

    private fun processBack() {
        AlertDialog.Builder(this).setMessage("保存修改？")
            .setPositiveButton("确定") { _: DialogInterface, _: Int ->
                setResult(getProvider())
            }.setNegativeButton("取消") { _: DialogInterface, _: Int ->
                finish()
            }.show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            processBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun getProvider(): LineProvider.ProviderInfo {
        return LineProvider.ProviderInfo(
            site = item_provider_site.text.toString(),
            title = item_provider_title.text.toString(),
            color = color,
            type = type,
            code = JsonUtil.toJson(adapter?.provider)
        )
    }

    private fun setResult(info: LineProvider.ProviderInfo?) {
        val intent = Intent()
        if (info != null) intent.putExtra(EXTRA_PROVIDER_INFO, JsonUtil.toJson(info))
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    val clipboardManager by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> processBack()
            R.id.action_remove -> {
                AlertDialog.Builder(this).setMessage("删除这个接口？")
                    .setPositiveButton("确定") { _: DialogInterface, _: Int ->
                        setResult(null)
                    }.show()
            }
            R.id.action_submit -> {
                setResult(getProvider())
            }
            R.id.action_inport -> {
                clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()?.let {
                    setProvider(it)
                } ?: {
                    Snackbar.make(root_layout, "剪贴板没有数据", Snackbar.LENGTH_LONG).show()
                }()
            }
            R.id.action_export -> {
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(
                        "videoplayer.providerInfo",
                        JsonUtil.toJson(getProvider())
                    )
                )
                Snackbar.make(root_layout, "数据已导出至剪贴板", Snackbar.LENGTH_LONG).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_edit, menu)
        if (intent?.getStringExtra(EXTRA_PROVIDER_INFO) == null) {
            menu?.findItem(R.id.action_remove)?.isVisible = false
        }
        return true
    }

    companion object {
        const val EXTRA_PROVIDER_TYPE = "extraProviderType"
        const val EXTRA_PROVIDER_INFO = "extraProvider"
    }
}
