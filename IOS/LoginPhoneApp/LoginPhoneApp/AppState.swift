//
//  AppState.swift
//  LoginPhoneApp
//
//  Orquestra o fluxo: cadastro -> verificação por SMS -> Home, com auto-login.
//

import Foundation
import SwiftUI
internal import Combine

enum Phase: Equatable {
    case launching     // checando sessão salva (auto-login)
    case signUp        // tela de cadastro (nome, email, senha, telefone)
    case verification  // tela de código SMS
    case home          // logado
}

@MainActor
final class AppState: ObservableObject {
    //let objectWillChange: ObservableObjectPublisher
    

    @Published var phase: Phase = .launching
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var infoMessage: String?

    @Published var signUp = SignUpData()
    @Published var code = ""
    @Published var user: UserResponse?

    private let api = APIClient()
    private var uuid: String { DeviceID.current }

    // MARK: - Auto-login

    func bootstrap() async {
        guard SessionStore.hasSession, let phone = SessionStore.savedPhone else {
            phase = .signUp
            return
        }
        isLoading = true
        defer { isLoading = false }
        do {
            switch try await api.login(phone: phone, uuid: uuid) {
            case .authenticated(let u):
                user = u
                phase = .home
            case .confirmationRequired:
                // uuid mudou ou usuário inativo: precisa reconfirmar por SMS
                signUp.phone = phone
                infoMessage = "Enviamos um código por SMS para \(phone)."
                phase = .verification
            }
        } catch {
            // sem rede ou erro: começa pelo cadastro
            phase = .signUp
        }
    }

    // MARK: - Cadastro

    func submitSignUp() async {
        errorMessage = nil
        infoMessage = nil
        let phone = signUp.phone.trimmingCharacters(in: .whitespaces)
        guard !signUp.name.trimmingCharacters(in: .whitespaces).isEmpty else {
            errorMessage = "Informe seu nome."; return
        }
        guard isValidEmail(signUp.email) else {
            errorMessage = "Informe um email válido."; return
        }
        guard signUp.password.count >= 6 else {
            errorMessage = "A senha deve ter ao menos 6 caracteres."; return
        }
        guard !phone.isEmpty else {
            errorMessage = "Informe o telefone (ex: +5541999990000)."; return
        }

        isLoading = true
        defer { isLoading = false }
        do {
            switch try await api.login(phone: phone, uuid: uuid) {
            case .confirmationRequired:
                infoMessage = "Enviamos um código por SMS para \(phone)."
                phase = .verification
            case .authenticated(let u):
                // já estava ativo neste dispositivo: atualiza o perfil e vai pra Home
                user = try await api.updateUser(
                    id: u.id, name: signUp.name, email: signUp.email, password: signUp.password
                )
                SessionStore.save(userId: u.id, phone: phone)
                phase = .home
            }
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? error.localizedDescription
        }
    }

    // MARK: - Verificação por SMS

    func submitCode() async {
        errorMessage = nil
        let codeTrim = code.trimmingCharacters(in: .whitespaces)
        guard !codeTrim.isEmpty else { errorMessage = "Informe o código recebido."; return }

        isLoading = true
        defer { isLoading = false }
        do {
            let confirmed = try await api.confirm(phone: signUp.phone, uuid: uuid, code: codeTrim)
            // Salva os dados de cadastro (se houver) e a sessão
            if !signUp.name.isEmpty || !signUp.email.isEmpty || !signUp.password.isEmpty {
                user = try await api.updateUser(
                    id: confirmed.id,
                    name: signUp.name.isEmpty ? nil : signUp.name,
                    email: signUp.email.isEmpty ? nil : signUp.email,
                    password: signUp.password.isEmpty ? nil : signUp.password
                )
            } else {
                user = confirmed
            }
            SessionStore.save(userId: confirmed.id, phone: signUp.phone)
            code = ""
            infoMessage = nil
            phase = .home
        } catch {
            errorMessage = (error as? APIError)?.errorDescription ?? error.localizedDescription
        }
    }

    // MARK: - Logout

    func logout() {
        SessionStore.clear()
        user = nil
        code = ""
        signUp = SignUpData()
        errorMessage = nil
        infoMessage = nil
        phase = .signUp
    }

    func backToSignUp() {
        code = ""
        errorMessage = nil
        infoMessage = nil
        phase = .signUp
    }

    private func isValidEmail(_ email: String) -> Bool {
        let regex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
        return email.range(of: regex, options: .regularExpression) != nil
    }
}
