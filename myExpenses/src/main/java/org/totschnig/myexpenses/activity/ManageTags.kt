package org.totschnig.myexpenses.activity

import android.content.Intent
import android.os.Bundle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.fragment.TagList

class ManageTags: ProtectedFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(themeIdEditDialog)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tags)
        setupToolbar(true)
        setTitle(R.string.tags_create_or_select)
        configureFloatingActionButton(R.string.content_description_tags_confirm)
        floatingActionButton.setImageResource(R.drawable.ic_menu_done)
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (command == R.id.CREATE_COMMAND) {
            setResult(RESULT_OK, (supportFragmentManager.findFragmentById(R.id.tag_list) as TagList).resultIntent())
            finish()
        }
        return super.dispatchCommand(command, tag)
    }
}