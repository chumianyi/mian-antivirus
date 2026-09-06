package com.chumian.miansecurity.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.chumian.miansecurity.R
import com.chumian.miansecurity.core.ShizukuHelper
import com.chumian.miansecurity.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启动时检查Shizuku，未授权则跳转到引导页
        if (!ShizukuHelper.isReady(this)) {
            startActivity(Intent(this, ShizukuGuideActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_scan -> {
                    loadFragment(ScanFragment())
                    true
                }
                R.id.nav_about -> {
                    loadFragment(AboutFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun switchToScanTab() {
        binding.bottomNav.selectedItemId = R.id.nav_scan
    }
}
