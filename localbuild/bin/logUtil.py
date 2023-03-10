import logging


class Logging:
    def __init__(self, fileName):
        self.filename = fileName
        self.format = logging.Formatter('%(asctime)s - %(filename)s[line:%(lineno)d] - %(levelname)s: %(message)s')
        self.logger = logging.getLogger()
        self.logger.setLevel(logging.DEBUG)
        self.set_console_logger()
        self.set_file_logger()

    def set_console_logger(self):
        handler = logging.StreamHandler()
        handler.setLevel(logging.INFO)
        handler.setFormatter(self.format)
        self.logger.addHandler(handler)

    def set_file_logger(self):
        fileLog = logging.FileHandler(self.filename)
        fileLog.setLevel(logging.INFO)
        fileLog.setFormatter(self.format)
        self.logger.addHandler(fileLog)

    def get_logger(self):
        return self.logger

