package com.vutrankien.android.lib

import android.util.Log
import com.vutrankien.lib.LogFactory

class AndroidLogFactory: LogFactory {
    companion object {
        val instance: LogFactory = AndroidLogFactory()
    }
    override fun newLog(tag: String): LogFactory.Log {
        return object : LogFactory.Log {
            override fun v(msg: String) {
                Log.v(tag, msg)
            }

            override fun d(msg: String) {
                Log.d(tag, msg)
            }

            override fun i(msg: String) {
                Log.i(tag, msg)
            }

            override fun w(msg: String) {
                Log.w(tag, msg)
            }

            override fun e(msg: String) {
                Log.e(tag, msg)
            }

        }
    }
}