package com.vutrankien.bluecontrol

import com.vutrankien.android.lib.AndroidLogFactory
import com.vutrankien.lib.LogFactory
import org.koin.dsl.module

val appDependencies = module {

    // Singleton (returns always the same unique instance of the object)
    //single<LogFactory> { AndroidLogFactory.instance }

    // Transient (returns always the a new instance of the object)
    // factory { FakeInMemoryUsersRepository() }

}