/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.instantexecution.initialization

import org.gradle.api.InvalidUserCodeException
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.Task
import org.gradle.api.internal.BuildScopeListenerRegistrationListener
import org.gradle.api.internal.GeneratedSubclasses
import org.gradle.api.internal.tasks.execution.TaskExecutionAccessListener
import org.gradle.instantexecution.problems.InstantExecutionProblems
import org.gradle.instantexecution.problems.PropertyProblem
import org.gradle.instantexecution.problems.PropertyTrace
import org.gradle.instantexecution.problems.StructuredMessage
import org.gradle.internal.InternalListener


class InstantExecutionProblemsListener internal constructor(

    private
    val startParameter: InstantExecutionStartParameter,

    private
    val problems: InstantExecutionProblems

) : TaskExecutionAccessListener, BuildScopeListenerRegistrationListener {

    override fun onProjectAccess(invocationDescription: String, task: Task) {
        onTaskExecutionAccessProblem(invocationDescription, task)
    }

    override fun onTaskDependenciesAccess(invocationDescription: String, task: Task) {
        onTaskExecutionAccessProblem(invocationDescription, task)
    }

    private
    fun onTaskExecutionAccessProblem(invocationDescription: String, task: Task) {
        if (startParameter.isEnabled) {
            val exception = InvalidUserCodeException(
                "Invocation of '$invocationDescription' by $task at execution time is unsupported."
            )
            problems.onProblem(taskExecutionAccessProblem(
                PropertyTrace.Task(GeneratedSubclasses.unpackType(task), task.path),
                invocationDescription,
                exception
            ))
        }
    }

    private
    fun taskExecutionAccessProblem(trace: PropertyTrace, invocationDescription: String, exception: InvalidUserCodeException) =
        PropertyProblem(
            trace,
            StructuredMessage.build {
                text("invocation of ")
                reference(invocationDescription)
                text(" at execution time is unsupported.")
            },
            exception
        )

    override fun onBuildScopeListenerRegistration(listener: Any, invocationDescription: String, invocationSource: Any) {
        if (startParameter.isEnabled && listener !is InternalListener && listener !is ProjectEvaluationListener) {
            val exception = InvalidUserCodeException(
                "Listener registration '$invocationDescription' by $invocationSource is unsupported."
            )
            problems.onProblem(listenerRegistrationProblem(
                PropertyTrace.Unknown,
                invocationDescription,
                exception
            ))
        }
    }

    private
    fun listenerRegistrationProblem(
        trace: PropertyTrace,
        invocationDescription: String,
        exception: InvalidUserCodeException
    ) =
        PropertyProblem(
            trace,
            StructuredMessage.build {
                text("registration of listener on ")
                reference(invocationDescription)
                text(" is unsupported")
            },
            exception
        )
}
