//
//  SignUpView.swift
//  LoginPhoneApp
//
//  Tela de cadastro: nome, email, senha e telefone.
//

import SwiftUI

struct SignUpView: View {
    @EnvironmentObject var app: AppState

    var body: some View {
        NavigationStack {
            Form {
                Section("Seus dados") {
                    TextField("Nome", text: $app.signUp.name)
                        .textContentType(.name)

                    TextField("Email", text: $app.signUp.email)
                        .textContentType(.emailAddress)
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()

                    SecureField("Senha (mín. 6 caracteres)", text: $app.signUp.password)
                        .textContentType(.newPassword)

                    TextField("Telefone (ex: +5541999990000)", text: $app.signUp.phone)
                        .textContentType(.telephoneNumber)
                        .keyboardType(.phonePad)
                }

                if let info = app.infoMessage {
                    Text(info).foregroundStyle(.blue)
                }
                if let error = app.errorMessage {
                    Text(error).foregroundStyle(.red)
                }

                Section {
                    Button {
                        Task { await app.submitSignUp() }
                    } label: {
                        if app.isLoading {
                            ProgressView().frame(maxWidth: .infinity)
                        } else {
                            Text("Cadastrar").frame(maxWidth: .infinity)
                        }
                    }
                    .disabled(app.isLoading)
                }
            }
            .navigationTitle("Criar conta")
        }
    }
}

#Preview {
    SignUpView().environmentObject(AppState())
}
