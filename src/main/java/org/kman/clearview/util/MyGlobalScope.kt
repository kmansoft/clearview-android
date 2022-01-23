package org.kman.clearview.util

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

object MyGlobalScope {
	private val job = Job()
	private val coroutineContext = job + Dispatchers.Main
	private val scope = CoroutineScope(coroutineContext)

	fun launch(context: CoroutineContext,
			   block: suspend CoroutineScope.() -> Unit): Job {
		return scope.launch(context = context, block = block)
	}
}
