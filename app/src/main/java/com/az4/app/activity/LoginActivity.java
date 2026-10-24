package com.az4.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.az4.app.R;
import com.az4.app.db.DatabaseHelper;
import com.az4.app.entity.User;
import com.az4.app.utils.SPUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * 登录页（启动页）。
 * 支持账号密码登录、注册弹窗、登录状态持久化与自动免登录。
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilAccount;
    private TextInputLayout tilPassword;
    private TextInputEditText etAccount;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private TextView tvGoRegister;

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 自动免登录：已登录直接进入首页，并销毁登录页
        if (SPUtils.isLogin(this)) {
            goHome();
            return;
        }
        setContentView(R.layout.activity_login);
        db = DatabaseHelper.getInstance(this);
        initViews();
        initListeners();
    }

    private void initViews() {
        tilAccount = findViewById(R.id.til_account);
        tilPassword = findViewById(R.id.til_password);
        etAccount = findViewById(R.id.et_account);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoRegister = findViewById(R.id.tv_go_register);
    }

    private void initListeners() {
        btnLogin.setOnClickListener(v -> doLogin());
        tvGoRegister.setOnClickListener(v -> showRegisterDialog());
    }

    /** 登录校验与跳转 */
    private void doLogin() {
        String account = text(etAccount);
        String password = text(etPassword);

        tilAccount.setError(null);
        tilPassword.setError(null);

        if (TextUtils.isEmpty(account)) {
            tilAccount.setError("请输入账号");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("请输入密码");
            return;
        }

        User user = db.login(account, password);
        if (user == null) {
            tilPassword.setError("账号或密码不正确");
            return;
        }

        // 保存登录状态，下次冷启动自动跳过登录页
        String nickname = TextUtils.isEmpty(user.getNickname()) ? user.getAccount() : user.getNickname();
        SPUtils.saveLoginInfo(this, user.getId(), user.getAccount(), nickname);
        Toast.makeText(this, "欢迎回来，" + nickname, Toast.LENGTH_SHORT).show();
        goHome();
    }

    private void goHome() {
        Intent intent = new Intent(this, BookHomeActivity.class);
        startActivity(intent);
        finish();
    }

    /** 弹出注册对话框 */
    private void showRegisterDialog() {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_register, null, false);

        final TextInputLayout tilRegAccount = content.findViewById(R.id.til_reg_account);
        final TextInputLayout tilRegNickname = content.findViewById(R.id.til_reg_nickname);
        final TextInputLayout tilRegPassword = content.findViewById(R.id.til_reg_password);
        final TextInputLayout tilRegConfirm = content.findViewById(R.id.til_reg_confirm_pwd);
        final TextInputEditText etRegAccount = content.findViewById(R.id.et_reg_account);
        final TextInputEditText etRegNickname = content.findViewById(R.id.et_reg_nickname);
        final TextInputEditText etRegPassword = content.findViewById(R.id.et_reg_password);
        final TextInputEditText etRegConfirm = content.findViewById(R.id.et_reg_confirm_pwd);

        final AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("用户注册")
                .setView(content)
                .setPositiveButton("注册", null)
                .setNegativeButton("取消", null)
                .create();

        // 手动覆盖正按钮点击事件，实现"校验失败不关闭弹窗"
        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                String account = text(etRegAccount);
                String nickname = text(etRegNickname);
                String password = text(etRegPassword);
                String confirm = text(etRegConfirm);

                tilRegAccount.setError(null);
                tilRegNickname.setError(null);
                tilRegPassword.setError(null);
                tilRegConfirm.setError(null);

                if (TextUtils.isEmpty(account)) {
                    tilRegAccount.setError("请输入账号");
                    return;
                }
                if (account.length() < 3) {
                    tilRegAccount.setError("账号至少 3 个字符");
                    return;
                }
                if (db.isAccountExist(account)) {
                    tilRegAccount.setError("该账号已被注册");
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    tilRegPassword.setError("请输入密码");
                    return;
                }
                if (password.length() < 4) {
                    tilRegPassword.setError("密码至少 4 位");
                    return;
                }
                if (!password.equals(confirm)) {
                    tilRegConfirm.setError("两次输入的密码不一致");
                    return;
                }
                if (TextUtils.isEmpty(nickname)) {
                    nickname = account; // 昵称选填，默认取账号
                }

                User user = new User(account, nickname, password);
                long id = db.insertUser(user);
                if (id <= 0) {
                    tilRegAccount.setError("注册失败，请重试");
                    return;
                }

                // 注册成功后自动回填登录表单，方便直接登录
                etAccount.setText(account);
                etPassword.setText(password);
                Toast.makeText(LoginActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private String text(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
