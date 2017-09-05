package br.com.bhl.superfid;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import br.com.bhl.superfid.main.Usuario;
import br.com.bhl.superfid.util.Mask;

public class CadastrarActivity extends ComumActivity implements DatabaseReference.CompletionListener {

    private AutoCompleteTextView edt_nome;
    private AutoCompleteTextView edt_sobrenome;
    private AutoCompleteTextView edt_cpf;
    private EditText edt_ddd;
    private EditText edt_telefone;
    private EditText edt_dtnascimento;
    private AutoCompleteTextView edt_email;
    private EditText edt_senha;
    private Toolbar toolbar;

    private FirebaseAuth myAuth;
    private FirebaseAuth.AuthStateListener myAuthStateListener;
    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastrar);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Cadastro");
        setSupportActionBar(toolbar);

        myAuth = FirebaseAuth.getInstance();

        myAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                if (firebaseUser == null || usuario.getCodigo() != null) {
                    return;
                }

                usuario.setCodigo(firebaseUser.getUid());
                usuario.salvarDatabase(CadastrarActivity.this);
            }
        };

        initViews();
    }

    /**********************************************************************************
     *********************************************************************************/
    @Override
    protected void onStart() {
        super.onStart();
        myAuth.addAuthStateListener(myAuthStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (myAuthStateListener != null) {
            myAuth.removeAuthStateListener(myAuthStateListener);
        }
    }

    @Override
    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
        myAuth.signOut();

        showToast("Conta criada com sucesso!");
        closeProgressBar();
        finish();
    }

    /**********************************************************************************
     *********************************************************************************/
    protected void initViews() {
        edt_nome = (AutoCompleteTextView) findViewById(R.id.edt_nome);
        edt_sobrenome = (AutoCompleteTextView) findViewById(R.id.edt_sobrenome);
        edt_cpf = (AutoCompleteTextView) findViewById(R.id.edt_cpf);
        edt_cpf.addTextChangedListener(Mask.insert("###.###.###-##", edt_cpf));
        edt_ddd = (EditText) findViewById(R.id.edt_ddd);
        edt_telefone = (EditText) findViewById(R.id.edt_telefone);
        edt_dtnascimento = (EditText) findViewById(R.id.edt_dtnascimento);
        edt_email = (AutoCompleteTextView) findViewById(R.id.edt_email);
        edt_senha = (EditText) findViewById(R.id.edt_senha);
        progressBar = (ProgressBar) findViewById(R.id.cadastrar_progressbar);
    }

    protected void initUser() {
        usuario = new Usuario(  );

        usuario.setNome( edt_nome.getText().toString() );
        usuario.setSobrenome( edt_sobrenome.getText().toString() );

        if(!TextUtils.isEmpty(edt_cpf.getText().toString())) {
            usuario.setCpf(Long.parseLong( Usuario.tirarCaracteresEspeciais( edt_cpf.getText().toString() ) ) );
        } else {
            usuario.setCpf( 0 );
        }

        if(!TextUtils.isEmpty(edt_ddd.getText().toString())) {
            usuario.setDdd(Integer.parseInt(edt_ddd.getText().toString()));
        } else {
            usuario.setDdd( 0 );
        }

        if(!TextUtils.isEmpty(edt_telefone.getText().toString())) {
            usuario.setTelefone(Integer.parseInt(edt_telefone.getText().toString()));
        } else {
            usuario.setTelefone( 0 );
        }

        if (!TextUtils.isEmpty(edt_dtnascimento.getText().toString())) {
            SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy");
            try {
                Calendar data = Calendar.getInstance();
                data.setTime(formatoData.parse(edt_dtnascimento.getText().toString()));
                usuario.setDataNascimento(data);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            edt_dtnascimento.setText( null );
        }

        usuario.setEmail(edt_email.getText().toString());
        usuario.setSenha(edt_senha.getText().toString());

    }

    /**********************************************************************************
     *********************************************************************************/
    public void enviarDadosCadastro(View view) {
        initUser();
        cadastrarUsuario();
    }

    private void cadastrarUsuario() {

        if (!validarForm()) {
            return;
        }

        openProgressBar();

        myAuth.createUserWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (!task.isSuccessful()) {
                    closeProgressBar();
                }
            }
        }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                FirebaseCrash.report(e);
                showSnackbar(e.getMessage());
            }
        });
    }

    private boolean validarForm() {
        boolean valido = true;

        String nome = edt_nome.getText().toString();
        if (TextUtils.isEmpty(nome)) {
            edt_nome.setError("Obrigatório");
            valido = false;
        } else {
            edt_nome.setError(null);
        }

        String cpf = edt_cpf.getText().toString();
        if (TextUtils.isEmpty(cpf)) {
            edt_cpf.setError("Obrigatório");
            valido = false;
        } else {
            edt_cpf.setError(null);
        }

        String email = edt_email.getText().toString();
        if (TextUtils.isEmpty(email)) {
            edt_email.setError("Obrigatório");
            valido = false;
        } else {
            edt_email.setError(null);
        }

        String senha = edt_senha.getText().toString();
        if (TextUtils.isEmpty(senha)) {
            edt_senha.setError("Obrigatório");
            valido = false;
        } else {
            edt_senha.setError(null);
        }

        return valido;
    }

    public void showDatePicker(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            EditText edt_dtnascimento = (EditText) getActivity().findViewById(R.id.edt_dtnascimento);
            SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy");

            Calendar data = Calendar.getInstance();
            data.set(year, month, day);

            edt_dtnascimento.setText(formatoData.format(data.getTime()));
        }
    }

}//fim da classe