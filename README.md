# 🛒 BuyLog Backend Server

AI 기반 최저가 비교 및 구매 예측 기능을 제공하는 스프링 부트(Spring Boot) 백엔드 API 서버입니다. 다중 비동기 크롤링(Naver, SerpApi)과 LLM(OpenAI)을 활용하여 복잡한 E-커머스 데이터를 단품/세트 최저가로 정제하여 프론트엔드(Flutter)에 제공합니다.

## 🛠 Tech Stack
* **Framework:** Spring Boot
* **Language:** Java
* **AI & API:** OpenAI API, Naver Search API, SerpApi(Google Shopping)
* **Build Tool:** Gradle

## 🚀 Getting Started

보안을 위해 API 키가 포함된 파일은 깃허브에 올라가지 않습니다. 프로젝트를 Clone한 후, 로컬 환경에서 서버를 실행하기 위해 아래의 필수 세팅을 먼저 진행해 주세요.

### 1. 환경 변수(.env) 설정
프로젝트 최상위 디렉토리(build.gradle이 있는 위치)에 `.env` 파일을 생성하고 아래 내용을 입력합니다. (실제 키 값은 [노션 -> 환경변수]를 확인해 주세요.)

```env
OPENAI_API_KEY=sk-여기에_실제_API_키_입력
