from flask import Flask


def make_app():
    app = Flask(__name__)

    @app.route('/')
    def homepage():
        return 'Hello, world!'

    return app


if __name__ == '__main__':
    app = make_app()
    app.run(port=800)
