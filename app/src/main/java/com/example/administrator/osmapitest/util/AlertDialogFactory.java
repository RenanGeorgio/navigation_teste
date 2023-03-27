package com.example.administrator.osmapitest.util;

import android.content.Context;
import android.content.DialogInterface;


import androidx.appcompat.app.AlertDialog;

import com.example.administrator.osmapitest.shared.Status;


/**
 * Classe de ferramenta para construir caixas de alerta
 */
public class AlertDialogFactory {
    public static AlertDialog.Builder getNoMapDialog(Context context) {
        return new AlertDialog.Builder(context)
                .setTitle("dica do sistema")
                .setMessage("O mapa interno não existe na área atual").setCancelable(false)
                .setNegativeButton("Confirmar", null)
                .setPositiveButton("Nao mostrar novamente", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Status.setAlterIndoorMapIsNotExist(false);
                    }
                });
    }

    public static AlertDialog.Builder getNoNavInfoDialog(final Context context) {
        return new AlertDialog.Builder(context)
                .setTitle("dica do sistema")
                .setMessage("A área alvo não existe, por favor reconfirme").setCancelable(false)
                .setNegativeButton("Confirmar", null);
//                .setPositiveButton("发送错误信息", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        Toast.makeText(context, "已向服务器反映问题，我们会尽快解决", Toast.LENGTH_SHORT).show();
//                    }
//                });
    }
}
