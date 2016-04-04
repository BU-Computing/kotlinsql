/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * This file is licenced to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You should have received a copy of the license with the source distribution.
 * Alternatively, you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Created by pdvrieze on 04/04/16.
 */

package gen.selects

const val count=10

fun main(args:Array<String>) {
  for(n in 2..count) {
    println()
    print("  class _Select$n<")
    (1..n).joinToString(",\n                 ") { m -> "T$m:Any, S$m:IColumnType<T$m,S$m,C$m>, C$m: Column<T$m, S$m, C$m>" }.apply{print(this)}

    print(">(")
    print((1..n).joinToString { m -> "col$m: C$m" })
    print("):\n        _BaseSelect(")
    print((1..n).joinToString { m -> "col$m" })
    println("){")
    println("      override fun WHERE(config: _Where.() -> WhereClause): Statement =")
    println("          _Statement$n(this, _Where().config())")
    println("  }")

  }
}