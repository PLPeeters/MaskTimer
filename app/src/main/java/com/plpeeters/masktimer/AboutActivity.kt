package com.plpeeters.masktimer

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.plpeeters.masktimer.databinding.ActivityAboutBinding
import com.plpeeters.masktimer.utils.getVersionName


class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.version.text = resources.getString(R.string.app_version, getVersionName())

        for (viewWithLinks in listOf(binding.aboutIcon, binding.aboutAuthor)) {
            viewWithLinks.movementMethod = LinkMovementMethod()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                super.onBackPressed()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
