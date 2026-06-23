//
//  Models.swift
//  LoginPhoneApp
//
//  Modelos de request/response da API do AuthServer.
//

import Foundation

struct LoginRequest: Encodable {
    let phone: String
    let uuid: String
}

struct ConfirmRequest: Encodable {
    let phone: String
    let uuid: String
    let code: String
}

struct UpdateUserRequest: Encodable {
    let name: String?
    let email: String?
    let password: String?
    let description: String?
}

struct UserResponse: Decodable, Identifiable {
    let id: String
    let phone: String
    let uuid: String
    let active: Bool
    let name: String?
    let email: String?
    let description: String?
    let avatar: String?
}

/// Dados coletados na tela de cadastro, antes da verificação por SMS.
struct SignUpData {
    var name: String = ""
    var email: String = ""
    var password: String = ""
    var phone: String = ""
}
