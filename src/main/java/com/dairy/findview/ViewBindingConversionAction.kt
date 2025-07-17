package com.dairy.findview

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.MessageType
import org.jetbrains.kotlin.psi.KtFile

class ViewBindingConversionAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        try {
            val psiFile = e.getData(CommonDataKeys.PSI_FILE)
            psiFile ?: return
            val editor = e.getData(CommonDataKeys.EDITOR)
            val layoutFile = Utils.getFileFromCaret(psiFile, editor)
            val resBeans = Utils.getResBeanFromFile(psiFile, editor)
            val dialog = MergeDialog(resBeans)
            dialog.setClickListener {
                val factory: BaseViewCreateFactory
                if (psiFile is KtFile) {
                    val ktClass = Utils.getPsiClassFromEvent(editor)
                    factory = KtViewMergeFactory(resBeans, psiFile, layoutFile, ktClass)
                } else {
                    val psiClass = Utils.getTargetClass(editor, psiFile)
                    factory = JavaViewMergeFactory(resBeans, psiFile, layoutFile, psiClass)
                }
                if (resBeans.isEmpty()) {
                    Utils.showNotification(
                        psiFile.project,
                        MessageType.WARNING,
                        "No layout found or no resource IDs found in layout"
                    )
                } else {
                    factory.execute()
                }
            }
            dialog.pack()
            dialog.setLocationRelativeTo(null) //居中
            dialog.isVisible = true
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}
