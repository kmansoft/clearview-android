@file:Suppress("DEPRECATION")

package org.kman.clearview.ui.login

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.kman.clearview.MainActivity
import org.kman.clearview.R
import org.kman.clearview.core.AuthInfo


class LoginFragment : Fragment() {

    private val mModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_login, container, false)
        mEditServer = root.findViewById(R.id.login_server)
        mEditUsername = root.findViewById(R.id.login_username)
        mEditPassword = root.findViewById(R.id.login_password)
        mCheckPlainHttp = root.findViewById(R.id.login_plain_http)
        mButtonLogin = root.findViewById(R.id.login_button)

        mEditServer.doAfterTextChanged {
            updateLoginButton()
        }
        mEditUsername.doAfterTextChanged {
            updateLoginButton()
        }
        mEditPassword.doAfterTextChanged {
            updateLoginButton()
        }

        mButtonLogin.isEnabled = false
        mButtonLogin.setOnClickListener(this::onClickLogin)

        mModel.progress.observe(viewLifecycleOwner) {
            showProgress(it)
        }
        mModel.error.observe(viewLifecycleOwner) {
            showError(it)
        }
        mModel.auth.observe(viewLifecycleOwner) {
            authIsCompleted(it)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        mProgressDialog?.dismiss()
        mProgressDialog = null

        mErrorDialog?.dismiss()
        mErrorDialog = null
    }

    private fun updateLoginButton() {
        val server = mEditServer.text.toString().trim()
        val username = mEditUsername.text.toString().trim()
        val password = mEditPassword.text.toString().trim()

        mButtonLogin.isEnabled = server.isNotEmpty() &&
                username.isEmpty() == password.isEmpty()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onClickLogin(view: View) {
        val server = mEditServer.text.toString().trim()
        if (server.isEmpty()) {
            return
        }

        val authInfo = AuthInfo(
            server,
            mEditUsername.text.toString().trim(),
            mEditPassword.text.toString().trim(),
            mCheckPlainHttp.isChecked)

        mModel.startAuth(authInfo)
    }

    private fun authIsCompleted(auth: AuthInfo?) {
        if (auth != null) {
            val activity = activity as MainActivity?
            activity?.also {
                it.onAuthInfoLoaded(auth)
            }
        }
    }

    private fun onDismissDialog(dialog: DialogInterface) {
        when (dialog) {
            mProgressDialog -> mProgressDialog = null
            mErrorDialog -> mErrorDialog = null
        }
    }

    @Suppress("DEPRECATION")
    private fun showProgress(show: Boolean) {
        if (show) {
            val context = activity ?: return
            var dialog = mProgressDialog
            if (dialog == null) {
                dialog = ProgressDialog.show(
                    context, context.getString(R.string.please_wait),
                    context.getString(R.string.login_progress_message)
                )
                dialog?.setOnDismissListener(this::onDismissDialog)
                mProgressDialog = dialog
            }

            mProgressDialog?.show()
        } else {
            mProgressDialog?.dismiss()
            mProgressDialog = null
        }
    }

    private fun showError(msg: String?) {
        if (msg != null) {
            val context = activity ?: return
            var dialog = mErrorDialog
            if (dialog == null) {
                dialog = AlertDialog.Builder(context).apply {
                    setTitle(R.string.error_title)
                    setMessage(msg)
                }.show()
                dialog.setOnDismissListener(this::onDismissDialog)
                mErrorDialog = dialog
            }

            mErrorDialog?.setMessage(msg)
            mErrorDialog?.show()
        } else {
            mErrorDialog?.dismiss()
            mErrorDialog = null
        }
    }

    private lateinit var mEditServer: EditText
    private lateinit var mEditUsername: EditText
    private lateinit var mEditPassword: EditText
    private lateinit var mCheckPlainHttp: CheckBox
    private lateinit var mButtonLogin: Button

    @Suppress("DEPRECATION")
    private var mProgressDialog: ProgressDialog? = null
    private var mErrorDialog: AlertDialog? = null
}

