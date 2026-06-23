//
//  APIClient.swift
//  LoginPhoneApp
//
//  Cliente HTTP (URLSession) para o AuthServer.
//

import Foundation

/// Resultado de POST /users/login.
enum LoginOutcome {
    case authenticated(UserResponse)   // 200: telefone+uuid já ativos
    case confirmationRequired          // 202: SMS enviado, precisa confirmar
}

enum APIError: LocalizedError {
    case server(Int)
    case noPendingCode        // 404 no confirm
    case invalidCode          // 400 no confirm
    case decoding
    case network(String)

    var errorDescription: String? {
        switch self {
        case .server(let code): return "Falha no servidor (HTTP \(code))."
        case .noPendingCode: return "Nenhum código pendente. Refaça o cadastro/login."
        case .invalidCode: return "Código inválido ou expirado."
        case .decoding: return "Resposta inesperada do servidor."
        case .network(let m): return "Erro de rede: \(m)"
        }
    }
}

struct APIClient {

    // Simulador iOS: localhost aponta para o Mac, então funciona direto.
    // Dispositivo físico: troque por http://SEU_IP_NA_REDE:8080 (e configure ATS).
    static let baseURL = URL(string: "http://192.168.68.52:8080")!

    private let session = URLSession.shared

    // MARK: - POST /users/login

    func login(phone: String, uuid: String) async throws -> LoginOutcome {
        let body = LoginRequest(phone: phone, uuid: uuid)
        let (data, http) = try await post(path: "users/login", body: body)
        switch http.statusCode {
        case 202:
            return .confirmationRequired
        case 200:
            guard let user = try? JSONDecoder().decode(UserResponse.self, from: data) else {
                throw APIError.decoding
            }
            return .authenticated(user)
        default:
            throw APIError.server(http.statusCode)
        }
    }

    // MARK: - POST /users/confirm

    func confirm(phone: String, uuid: String, code: String) async throws -> UserResponse {
        let body = ConfirmRequest(phone: phone, uuid: uuid, code: code)
        let (data, http) = try await post(path: "users/confirm", body: body)
        switch http.statusCode {
        case 200:
            guard let user = try? JSONDecoder().decode(UserResponse.self, from: data) else {
                throw APIError.decoding
            }
            return user
        case 404:
            throw APIError.noPendingCode
        case 400:
            throw APIError.invalidCode
        default:
            throw APIError.server(http.statusCode)
        }
    }

    // MARK: - PUT /users/{id}

    @discardableResult
    func updateUser(
        id: String, name: String?, email: String?, password: String?, description: String? = nil
    ) async throws -> UserResponse {
        let body = UpdateUserRequest(name: name, email: email, password: password, description: description)
        let (data, http) = try await send(method: "PUT", path: "users/\(id)", body: body)
        guard http.statusCode == 200,
              let user = try? JSONDecoder().decode(UserResponse.self, from: data) else {
            throw APIError.server(http.statusCode)
        }
        return user
    }

    // MARK: - Helpers

    private func post<T: Encodable>(path: String, body: T) async throws -> (Data, HTTPURLResponse) {
        try await send(method: "POST", path: path, body: body)
    }

    private func send<T: Encodable>(
        method: String, path: String, body: T
    ) async throws -> (Data, HTTPURLResponse) {
        var request = URLRequest(url: APIClient.baseURL.appendingPathComponent(path))
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(body)
        do {
            let (data, response) = try await session.data(for: request)
            guard let http = response as? HTTPURLResponse else { throw APIError.decoding }
            return (data, http)
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.network(error.localizedDescription)
        }
    }
}
