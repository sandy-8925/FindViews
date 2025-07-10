package com.dairy.findview

import org.apache.commons.lang3.StringUtils
import java.util.regex.Pattern

class ResBean(name: String, id: String?) {
    @JvmField
    var isChecked: Boolean = true
    @JvmField
    var name: String? = null
    private var fullName: String? = null
    private var isSystem = false
    lateinit var id: String
    private var nameType = 3 //aa_bb=1,aaBb=2,mAaBb=3

    init {
        val matcher = sIdPattern.matcher(id)
        if (matcher.find() && matcher.groupCount() > 1) {
            this.id = matcher.group(2)
            val group = matcher.group(1)
            isSystem = !(group == null || group.length == 0)
        }
        requireNotNull(this.id) { "Invalid format of view id" }
        val packages = name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (packages.size > 1) {
            this.fullName = name
            this.name = packages[packages.size - 1]
        } else {
            this.fullName = name
            this.name = name
        }
    }

    fun setNameType(nameType: Int) {
        this.nameType = nameType
    }

    val fieldName: String
        get() = getFieldName(nameType)

    fun getFieldName(nameType: Int): String {
        var fieldName = id
        val names = id.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (nameType == 3) {
            val sb = StringBuilder("m")
            names.forEach { sb.append(StringUtils.capitalize(it)) }
            fieldName = sb.toString()
        } else if (nameType == 2) {
            val sb = StringBuilder()
            names.forEachIndexed { index, s -> if(index == 0) sb.append(s) else sb.append(StringUtils.capitalize(s)) }
            fieldName = sb.toString()
        }
        return fieldName
    }

    val fullId: String
        get() = getFullId(false)

    private fun getFullId(isR2: Boolean): String {
        if (isSystem) {
            return "android.R.id.$id"
        }
        return (if (isR2) "R2.id." else "R.id.") + this.id
    }

    val javaButterKnifeFiled: String
        get() = CodeConstant.getJavaButterKnifeFiled(
            name,
            fieldName,
            getFullId(Config.get().isButterKnifeR2)
        )

    val adapterJavaButterKnifeFiled: String
        get() = CodeConstant.getAdapterJavaButterKnifeFiled(
            name,
            fieldName,
            getFullId(Config.get().isButterKnifeR2)
        )

    val kotlinButterKnifeProperty: String
        get() = CodeConstant.getKotlinButterKnifeProperty(
            name,
            fieldName,
            getFullId(Config.get().isButterKnifeR2)
        )

    val adapterKotlinButterKnifeProperty: String
        get() = CodeConstant.getAdapterKotlinButterKnifeProperty(
            name,
            fieldName,
            getFullId(Config.get().isButterKnifeR2)
        )

    val javaFiled: String
        get() = CodeConstant.getJavaFiled(name, fieldName)

    val adapterJavaFiled: String
        get() = CodeConstant.getAdapterJavaFiled(name, fieldName)

    fun getJavaStatement(view: String?): String {
        return CodeConstant.getJavaStatement(fieldName, view, fullId)
    }

    val kotlinProperty: String
        get() = CodeConstant.getKotlinProperty(name, fieldName)

    val kotlinAdapterProperty: String
        get() = CodeConstant.getKotlinAdapterProperty(name, fieldName)

    val adapterKotlinProperty: String
        get() = CodeConstant.getAdapterKotlinProperty(name, fieldName)

    fun getKotlinAdapterProperty(view: String?): String {
        return CodeConstant.getKotlinAdapterProperty(fieldName, name, view, fullId)
    }

    fun getKotlinLazyProperty(view: String?): String {
        return CodeConstant.getKotlinLazyProperty(fieldName, name, view, fullId)
    }

    fun getKotlinExpression(view: String?): String {
        return CodeConstant.getKotlinExpression(fieldName, view, fullId)
    }

    companion object {
        private val sIdPattern: Pattern =
            Pattern.compile("@\\+?(android:)?id/([^$]+)$", Pattern.CASE_INSENSITIVE)
    }
}
