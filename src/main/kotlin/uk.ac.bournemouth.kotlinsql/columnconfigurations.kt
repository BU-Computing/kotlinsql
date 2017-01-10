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

package uk.ac.bournemouth.kotlinsql

import uk.ac.bournemouth.kotlinsql.ColumnType.*
import java.math.BigDecimal
import kotlin.reflect.KProperty

/**
 * Class to abstract the column configuration of a resultset
 */
@Suppress("unused")
abstract class AbstractColumnConfiguration<T:Any, S: IColumnType<T, S, C>, C: Column<T, S, C>, out CONF_T:Any> {

  val table: TableRef
  var name: String?
  val type: S
  var notnull: Boolean?
  var unique: Boolean
  var autoincrement: Boolean
  var default: T?
  var comment:String?
  var columnFormat: ColumnFormat?
  var storageFormat: StorageFormat?
  var references: ColsetRef?

  constructor(table: TableRef, name: String? = null, type: S) {
    this.table = table
    this.name = name
    this.type = type
    this.notnull=null
    this.unique=false
    this.autoincrement=false
    this.default=null
    this.comment=null
    this.columnFormat=null
    this.storageFormat=null
    this.references=null
  }

  constructor(orig: AbstractColumnConfiguration<T, S, C, CONF_T>,
              table: TableRef,
              newName: String? = null) {
    this.table = table
    name = newName
    type = orig.type
    notnull = orig.notnull
    unique = orig.unique
    autoincrement = orig.autoincrement
    default = orig.default
    comment = orig.comment
    columnFormat = orig.columnFormat
    storageFormat = orig.storageFormat
    references = orig.references
  }

  inline operator fun provideDelegate(thisRef: MutableTable, property: KProperty<*>): Table.FieldAccessor<T,S,C> {
    if(name.isNullOrBlank()) { name = property.name }
    return thisRef.add(this.newColumn())
  }

  enum class ColumnFormat { FIXED, MEMORY, DEFAULT }
  enum class StorageFormat { DISK, MEMORY, DEFAULT }

  val NULL:Unit get() { notnull=false }
  val NOT_NULL:Unit get() { notnull = true }
  val AUTO_INCREMENT:Unit get() { autoincrement = true }
  val UNIQUE:Unit get() { unique = true }

  inline fun DEFAULT(value:T) { default=value }
  inline fun COMMENT(comment:String) { this.comment = comment }
  inline fun COLUMN_FORMAT(format: ColumnFormat) { columnFormat = format }
  inline fun STORAGE(format: StorageFormat) { storageFormat = format }
  inline fun REFERENCES(table: TableRef, col1: ColumnRef<*,*,*>, vararg columns: ColumnRef<*,*,*>) {
    references= ColsetRef(table, col1, *columns)
  }

  abstract fun copy(table: TableRef, newName: String?=null): CONF_T

  abstract fun newColumn():C

  class NormalColumnConfiguration<T:Any, S: SimpleColumnType<T, S>> : AbstractColumnConfiguration<T, S, SimpleColumn<T, S>, NormalColumnConfiguration<T, S>> {

    constructor(table: TableRef, name: String? = null, type: S) : super(table, name, type)

    constructor(orig: NormalColumnConfiguration<T, S>,
                table: TableRef,
                newName: String? = null): super(orig, table, newName)

    override fun newColumn(): SimpleColumn<T, S> = NormalColumnImpl(effectiveName, this)

    override fun copy(table: TableRef, newName: String?) = NormalColumnConfiguration(this, table, newName)
  }

  sealed abstract class AbstractNumberColumnConfiguration<T:Any, S: INumericColumnType<T, S,C>, C: INumericColumn<T, S, C>, out CONF_T:Any> : AbstractColumnConfiguration<T, S, C, CONF_T> {
    constructor(table: TableRef, name: String? = null, type: S) : super(table, name, type) {
      this.displayLength = -1
    }

    constructor(orig: AbstractNumberColumnConfiguration<T, S, C, CONF_T>, table:TableRef, newName: String?=null) : super(orig, table, newName) {
      unsigned = orig.unsigned
      zerofill = orig.zerofill
      displayLength = orig.displayLength
    }


    var unsigned: Boolean = false
    var zerofill: Boolean = false
    var displayLength: Int

    val UNSIGNED:Unit get() { unsigned = true }

    val ZEROFILL:Unit get() { unsigned = true }

    class NumberColumnConfiguration<T:Any, S: NumericColumnType<T, S>> : AbstractNumberColumnConfiguration<T, S, NumericColumn<T, S>, NumberColumnConfiguration<T, S>> {
      constructor(table: TableRef, name: String? = null, type: S) : super(table, name, type)
      constructor(orig: NumberColumnConfiguration<T, S>, table:TableRef, newName: String?=null) : super(orig, table, newName)

      override fun newColumn(): NumericColumn<T, S> = NumberColumnImpl(effectiveName, this)

      override fun copy(table: TableRef, newName: String?) = NumberColumnConfiguration(this, table, newName)
    }

    class DecimalColumnConfiguration<S: DecimalColumnType<S>> : AbstractNumberColumnConfiguration<BigDecimal, S, DecimalColumn<S>, DecimalColumnConfiguration<S>> {
      val precision: Int
      val scale: Int

      constructor(table: TableRef, name: String? = null, type: S, precision: Int= defaultPrecision, scale: Int = defaultScale) : super(table, name, type) {
        this.precision = precision
        this.scale = scale
      }

      constructor(orig: DecimalColumnConfiguration<S>, table:TableRef, newName: String?=null) : super(orig, table, newName) {
        precision = orig.precision
        scale = orig.scale
      }

      override fun newColumn(): DecimalColumn<S> = DecimalColumnImpl(effectiveName, this)
      override fun copy(table: TableRef, newName: String?) = DecimalColumnConfiguration(this, table, newName)

      companion object {
        const val defaultPrecision = 10
        const val defaultScale = 0
      }
    }

  }

  sealed abstract class AbstractCharColumnConfiguration<T:String, S: ICharColumnType<S, C>, C: ICharColumn<S, C>, out CONF_T:Any> : AbstractColumnConfiguration<String, S, C, CONF_T> {
    var charset: String? = null

    var collation: String? = null
    var binary:Boolean = false

    constructor(table: TableRef, name: String? = null, type: S) : super(table, name, type)
    constructor(orig: AbstractCharColumnConfiguration<String, S, C, CONF_T>, table:TableRef, newName: String?) : super(orig, table, newName) {
      charset = orig.charset
      collation = orig.collation
      binary = orig.binary
    }


    val BINARY:Unit get() { binary = true }

    inline fun CHARACTER_SET(charset:String) { this.charset = charset }
    inline fun COLLATE(collation:String) { this.collation = collation }

    class CharColumnConfiguration<S: CharColumnType<S>> : AbstractCharColumnConfiguration<String, S, CharColumn<S>, CharColumnConfiguration<S>> {
      constructor(table: TableRef, name: String? = null, type: S) : super(table, name, type)
      constructor(orig: CharColumnConfiguration<S>, table:TableRef, newName: String?=null) : super(orig, table, newName)

      override fun newColumn(): CharColumn<S> = CharColumnImpl(effectiveName, this)
      override fun copy(table: TableRef, newName: String?) = CharColumnConfiguration(this, table, newName)
    }

    class LengthCharColumnConfiguration<S: LengthCharColumnType<S>> : AbstractCharColumnConfiguration<String, S, LengthCharColumn<S>, LengthCharColumnConfiguration<S>>, BaseLengthColumnConfiguration<String, S, LengthCharColumn<S>> {
      override val length: Int

      constructor(table: TableRef, name: String? = null, type: S, length: Int) : super(table, name, type) {
        this.length = length
      }

      constructor(orig: LengthCharColumnConfiguration<S>, table:TableRef, newName: String? =null) : super(orig, table, newName) {
        this.length = orig.length
      }

      override fun newColumn(): LengthCharColumn<S> = LengthCharColumnImpl(effectiveName, this)
      override fun copy(table: TableRef, newName: String?) = LengthCharColumnConfiguration(this, table, newName)
    }
  }

  interface BaseLengthColumnConfiguration<T:Any, S: ILengthColumnType<T,S, C>, C:ILengthColumn<T,S,C>> {
    val length:Int
  }

  class LengthColumnConfiguration<T:Any, S: LengthColumnType<T, S>> : AbstractColumnConfiguration<T, S, LengthColumn<T, S>, LengthColumnConfiguration<T, S>>, BaseLengthColumnConfiguration<T, S, LengthColumn<T, S>> {
    override val length: Int

    constructor(table: TableRef, name: String? = null, type: S, length: Int) : super(table, name, type) {
      this.length = length
    }

    constructor(orig: LengthColumnConfiguration<T, S>, table:TableRef, newName: String?=null) : super(orig, table, newName) {
      length = orig.length
    }

    override fun newColumn(): LengthColumn<T, S> = LengthColumnImpl(effectiveName, this)
    override fun copy(table: TableRef, newName: String?) = LengthColumnConfiguration(this, table, newName)
  }
}


private inline val AbstractColumnConfiguration<*,*,*,*>.effectiveName: String get() = name?:  throw NullPointerException("database columns must have names")
