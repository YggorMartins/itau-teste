# Política de segurança

## Reporte de vulnerabilidades

Não publique detalhes exploráveis em uma issue pública. Use o recurso **Private vulnerability reporting** do GitHub quando ele estiver habilitado no repositório. Inclua impacto, pré-condições, passos mínimos de reprodução e uma sugestão de mitigação, sem dados pessoais ou credenciais reais.

Não existe SLA formal enquanto este repositório for um projeto de portfólio. Uma implantação real deve definir responsáveis, severidades, prazos de correção e canal 24x7.

## Regras obrigatórias de desenvolvimento seguro

1. Nunca commitar senhas, tokens, API keys, certificados, keystores ou dumps.
2. Nunca registrar body, `Authorization`, cookies, API keys, valores financeiros ou identificadores pessoais.
3. Todo input deve ter tipo, tamanho, formato e cardinalidade máximos definidos no servidor.
4. Não confiar em `X-Forwarded-For` fora de uma lista de proxies confiáveis.
5. Não usar conteúdo do header JWT para escolher dinamicamente o algoritmo de validação.
6. Não construir comandos, queries ou templates concatenando input do cliente.
7. Novos endpoints começam negados e recebem o menor escopo OAuth2 necessário.
8. Novas dependências exigem justificativa, licença aceitável, versão suportada e scan limpo.
9. Mensagens HTTP devem ser constantes e seguras; detalhes técnicos pertencem à telemetria protegida.
10. Alterações em autenticação, autorização, criptografia, workflows ou Docker exigem revisão AppSec.

## Gestão de vulnerabilidades

- Critical: bloquear release e iniciar correção imediata.
- High: bloquear merge/release, salvo exceção documentada e com prazo.
- Medium: registrar owner, mitigação e prazo.
- Low: avaliar no ciclo normal.

Exceções devem conter CVE/regra, componente, exposição real, controles compensatórios, responsável, data de expiração e aprovação. Não use listas de ignore sem prazo.

## Resposta a incidente

1. Conter acesso e preservar evidências.
2. Revogar/rotacionar credenciais possivelmente expostas.
3. Identificar versões, clientes e dados afetados.
4. Corrigir, testar e implantar por pipeline confiável.
5. Cumprir avaliação jurídica/regulatória e notificações aplicáveis.
6. Produzir análise de causa raiz e ações preventivas sem culpabilização individual.
