//
//  VerificationView.swift
//  LoginPhoneApp
//
//  Tela de verificação por SMS: o usuário digita o código recebido.
//

import SwiftUI

struct VerificationView: View {
    @EnvironmentObject var app: AppState

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Image(systemName: "lock.shield")
                    .font(.system(size: 56))
                    .foregroundStyle(.tint)

                Text("Confirme seu telefone")
                    .font(.title2).bold()

                Text("Digite o código que enviamos por SMS para \(app.signUp.phone).")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)

                TextField("Código de confirmação", text: $app.code)
                    .keyboardType(.numberPad)
                    .multilineTextAlignment(.center)
                    .font(.title3)
                    .textFieldStyle(.roundedBorder)

                if let error = app.errorMessage {
                    Text(error).foregroundStyle(.red).multilineTextAlignment(.center)
                }

                Button {
                    Task { await app.submitCode() }
                } label: {
                    if app.isLoading {
                        ProgressView().frame(maxWidth: .infinity)
                    } else {
                        Text("Verificar").frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(app.isLoading)

                Button("Voltar / corrigir dados") {
                    app.backToSignUp()
                }
                .disabled(app.isLoading)

                Spacer()
            }
            .padding()
            .navigationTitle("Verificação")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

#Preview {
    let app = AppState()
    app.signUp.phone = "+5541999990000"
    return VerificationView().environmentObject(app)
}
