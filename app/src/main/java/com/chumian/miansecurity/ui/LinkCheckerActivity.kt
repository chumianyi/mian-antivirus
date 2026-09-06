package com.chumian.miansecurity.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.chumian.miansecurity.R

class LinkCheckerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_tool)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "链接检测"
        toolbar.setNavigationOnClickListener { finish() }

        val content = findViewById<LinearLayout>(R.id.content)
        setupContent(content)
    }

    private fun setupContent(content: LinearLayout) {
        // 添加功能说明
        val desc = TextView(this).apply {
            text = "链接检测功能"
            textSize = 18f
            setPadding(0, 16, 0, 8)
        }
        content.addView(desc)

        // 添加功能内容（每个工具具体实现）
        addToolContent(content)
    }

    private fun addToolContent(content: LinearLayout) {
        // 具体工具内容由各Activity实现
        val info = TextView(this).apply {
            text = "链接检测功能正在运行中..."
            textSize = 14f
            setPadding(0, 8, 0, 8)
        }
        content.addView(info)

        // 添加操作按钮
        val button = com.google.android.material.button.MaterialButton(this).apply {
            text = "执行操作"
            setOnClickListener {
                android.widget.Toast.makeText(this@LinkCheckerActivity, "链接检测操作已执行", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        content.addView(button)
    }
}
