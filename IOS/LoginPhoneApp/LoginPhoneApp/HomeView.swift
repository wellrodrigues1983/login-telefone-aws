//
//  HomeView.swift
//  LoginPhoneApp
//
//  Tela principal exibida após a validação do telefone.
//

import SwiftUI

struct HomeView: View {
    @EnvironmentObject var app: AppState

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Image(systemName: "checkmark.seal.fill")
                    .font(.system(size: 64))
                    .foregroundStyle(.green)

                Text("Bem-vindo\(app.user?.name.map { ", \($0)" } ?? "")!")
                    .font(.title).bold()
                    .multilineTextAlignment(.center)

                if let user = app.user {
                    VStack(alignment: .leading, spacing: 8) {
                        Label(user.phone, systemImage: "phone")
                        if let email = user.email, !email.isEmpty {
                            Label(email, systemImage: "envelope")
                        }
                        Label(user.active ? "Telefone verificado" : "Pendente",
                              systemImage: user.active ? "checkmark.circle" : "exclamationmark.circle")
                    }
                    .font(.body)
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 12))
                }

                Spacer()

                Button(role: .destructive) {
                    app.logout()
                } label: {
                    Text("Sair").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
            }
            .padding()
            .navigationTitle("Home")
        }
    }
}

#Preview {
    let app = AppState()
    app.user = UserResponse(
        id: "1", phone: "+5541999990000", uuid: "uuid", active: true,
        name: "Wellington", email: "well@email.com", description: nil, avatar: nil
    )
    return HomeView().environmentObject(app)
}
